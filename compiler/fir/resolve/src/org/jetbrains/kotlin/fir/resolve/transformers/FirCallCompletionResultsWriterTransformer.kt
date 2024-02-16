/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildSamConversionExpression
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedCallableReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.dfa.FirDataFlowAnalyzer
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.inference.FirTypeVariablesAfterPCLATransformer
import org.jetbrains.kotlin.fir.resolve.inference.ResolvedLambdaAtom
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.*
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.impl.ConvertibleIntegerOperators.binaryOperatorsWithSignedArgument
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.scopes.impl.isWrappedIntegerOperator
import org.jetbrains.kotlin.fir.scopes.impl.isWrappedIntegerOperatorForUnsignedType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.resolve.calls.inference.model.InferredEmptyIntersection
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.collections.component1
import kotlin.collections.component2

class FirCallCompletionResultsWriterTransformer(
    override val session: FirSession,
    private val scopeSession: ScopeSession,
    private val finalSubstitutor: ConeSubstitutor,
    private val typeCalculator: ReturnTypeCalculator,
    private val typeApproximator: ConeTypeApproximator,
    private val dataFlowAnalyzer: FirDataFlowAnalyzer,
    private val integerOperatorApproximator: IntegerLiteralAndOperatorApproximationTransformer,
    private val samResolver: FirSamResolver,
    private val context: BodyResolveContext,
    private val mode: Mode = Mode.Normal,
) : FirAbstractTreeTransformer<ExpectedArgumentType?>(phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {

    private fun finallySubstituteOrNull(type: ConeKotlinType): ConeKotlinType? {
        val result = finalSubstitutor.substituteOrNull(type)
        if (result == null && type is ConeIntegerLiteralType) {
            return type.approximateIntegerLiteralType()
        }
        return result?.approximateIntegerLiteralType()
    }

    private fun finallySubstituteOrSelf(type: ConeKotlinType): ConeKotlinType {
        return finallySubstituteOrNull(type) ?: type
    }

    private val arrayOfCallTransformer = FirArrayOfCallTransformer()
    private var enableArrayOfCallTransformation = false

    enum class Mode {
        Normal, DelegatedPropertyCompletion
    }

    private inline fun <T> withFirArrayOfCallTransformer(block: () -> T): T {
        enableArrayOfCallTransformation = true
        return try {
            block()
        } finally {
            enableArrayOfCallTransformation = false
        }
    }

    private fun <T : FirQualifiedAccessExpression> prepareQualifiedTransform(
        qualifiedAccessExpression: T, calleeReference: FirNamedReferenceWithCandidate,
    ): T {
        val subCandidate = calleeReference.candidate

        subCandidate.updateSubstitutedMemberIfReceiverContainsTypeVariable()

        val declaration = subCandidate.symbol.fir
        val typeArguments = computeTypeArguments(qualifiedAccessExpression, subCandidate)
        val type = if (declaration is FirCallableDeclaration) {
            val calculated = typeCalculator.tryCalculateReturnType(declaration)
            if (calculated !is FirErrorTypeRef) {
                calculated.type
            } else {
                ConeErrorType(calculated.diagnostic)
            }
        } else {
            // this branch is for cases when we have
            // some invalid qualified access expression itself.
            // e.g. `T::toString` where T is a generic type.
            // in these cases we should report an error on
            // the calleeReference.source which is not a fake source.
            ConeErrorType(
                when (declaration) {
                    is FirTypeParameter -> ConeTypeParameterInQualifiedAccess(declaration.symbol)
                    else -> ConeSimpleDiagnostic("Callee reference to candidate without return type: ${declaration.render()}")
                }
            )
        }

        if (mode == Mode.DelegatedPropertyCompletion) {
            // Update type for `$delegateField` in `$$delegateField.get/setValue()` calls inside accessors
            val typeUpdater = TypeUpdaterForPCLAAndDelegateReceivers()
            qualifiedAccessExpression.transformExplicitReceiver(typeUpdater, null)
        }

        var dispatchReceiver = subCandidate.dispatchReceiverExpression()
        var extensionReceiver = subCandidate.chosenExtensionReceiverExpression()
        if (!declaration.isWrappedIntegerOperator()) {
            val expectedDispatchReceiverType = (declaration as? FirCallableDeclaration)?.dispatchReceiverType
            val expectedExtensionReceiverType = (declaration as? FirCallableDeclaration)?.receiverParameter?.typeRef?.coneType
            dispatchReceiver = dispatchReceiver?.transformSingle(integerOperatorApproximator, expectedDispatchReceiverType)
            extensionReceiver = extensionReceiver?.transformSingle(integerOperatorApproximator, expectedExtensionReceiverType)
        }

        if (subCandidate.usedOuterCs) {
            val updaterForThisReferences = TypeUpdaterForPCLAAndDelegateReceivers()
            dispatchReceiver = dispatchReceiver?.transformSingle(updaterForThisReferences, null)
            extensionReceiver = extensionReceiver?.transformSingle(updaterForThisReferences, null)
        }

        qualifiedAccessExpression.apply {
            replaceCalleeReference(calleeReference.toResolvedReference())
            replaceDispatchReceiver(dispatchReceiver)
            replaceExtensionReceiver(extensionReceiver)

            // If the explicit receiver is a smartcast expression, we can choose to use the unwrapped expression as dispatch receiver.
            // To maintain the invariant
            // explicitReceiver != null => explicitReceiver == dispatchReceiver || explicitReceiver == extensionReceiver
            // we update the explicit receiver here.
            // We only do this if the candidate is successful, otherwise, we can lose the explicit receiver node in red code like
            // fun f(s: String, action: (String.() -> Unit)?) {
            //    s.action?.let { it() }
            //}
            if (subCandidate.explicitReceiverKind == ExplicitReceiverKind.DISPATCH_RECEIVER && subCandidate.applicability.isSuccess) {
                replaceExplicitReceiver(dispatchReceiver)
            }
        }

        qualifiedAccessExpression.replaceContextReceiverArguments(subCandidate.contextReceiverArguments())

        subCandidate.diagnostics.firstIsInstanceOrNull<PropertyAsOperator>()?.let { propertyAsOperator ->
            val conePropertyAsOperator = ConePropertyAsOperator(propertyAsOperator.propertySymbol)
            val nonFatalDiagnostics: List<ConeDiagnostic> = buildList {
                addAll(qualifiedAccessExpression.nonFatalDiagnostics)
                add(conePropertyAsOperator)
            }
            qualifiedAccessExpression.replaceNonFatalDiagnostics(nonFatalDiagnostics)
        }

        qualifiedAccessExpression.replaceConeTypeOrNull(type)

        if (declaration !is FirErrorFunction) {
            qualifiedAccessExpression.replaceTypeArguments(typeArguments)
        }

        runPCLARelatedTasksForCandidate(subCandidate)

        session.lookupTracker?.recordTypeResolveAsLookup(type, qualifiedAccessExpression.source, context.file.source)
        return qualifiedAccessExpression
    }

    private fun runPCLARelatedTasksForCandidate(candidate: Candidate) {
        for (postponedCall in candidate.postponedPCLACalls) {
            postponedCall.transformSingle(this, null)
        }

        for (callback in candidate.onPCLACompletionResultsWritingCallbacks) {
            callback(finalSubstitutor)
        }

        // TODO: Be aware of exponent
        val firStubTypeTransformer = FirTypeVariablesAfterPCLATransformer(finalSubstitutor)
        for (lambda in candidate.lambdasAnalyzedWithPCLA) {
            lambda.transformSingle(firStubTypeTransformer, null)
        }
    }

    /**
     * Currently, it's only necessary for delegate inference, e.g. when the delegate expression returns some generic type
     * with non-fixed yet type variables and inside its member scope we find the `getValue` function that might still contain
     * the type variables, too and they even might be used to adding some constraints for them.
     *
     * After the completion ends and all the variables are fixed, this member candidate still contains them, so what this function does
     * is replace the candidate from Delegate<Tv, ...> scope to the same candidate from Delegate<ResultTypeForT, ..>.
     *
     * The fun fact is that it wasn't necessary before Delegate Inference refactoring because there were stub types left and FIR2IR
     * handled them properly as equal-to-anything unlike the type variable types.
     *
     * See codegen/box/delegatedProperty/noTypeVariablesLeft.kt
     *
     * That all looks a bit ugly, but there are not many options.
     * In an ideal world, we wouldn't have substitution overrides in FIR, but instead used a pair original symbol and substitution
     * everywhere, but we're not there yet.
     *
     * TODO: In future, it would be nice to get rid of it and there's actually a way to do it – not using substitution overrides (see KT-61618)
     */
    private fun Candidate.updateSubstitutedMemberIfReceiverContainsTypeVariable() {
        val updatedSymbol = symbol.updateSubstitutedMemberIfReceiverContainsTypeVariable(usedOuterCs) ?: return
        val oldSymbol = symbol

        @OptIn(Candidate.UpdatingSymbol::class)
        updateSymbol(updatedSymbol)

        check(updatedSymbol is FirCallableSymbol<*>)

        substitutor = substitutorByMap(
            updatedSymbol.typeParameterSymbols.zip(freshVariables).associate { (typeParameter, typeVariable) ->
                typeParameter to typeVariable.defaultType
            },
            session,
        )

        if (updatedSymbol !is FirFunctionSymbol) return
        require(oldSymbol is FirFunctionSymbol)

        val oldArgumentMapping = argumentMapping ?: return
        val oldValueParametersToNewMap = oldSymbol.valueParameterSymbols.zip(updatedSymbol.valueParameterSymbols).toMap()

        argumentMapping = oldArgumentMapping.mapValuesTo(linkedMapOf()) {
            oldValueParametersToNewMap[it.value.symbol]!!.fir
        }
    }

    private fun FirBasedSymbol<*>.updateSubstitutedMemberIfReceiverContainsTypeVariable(usedOuterCs: Boolean): FirBasedSymbol<*>? {
        // TODO: Add assertion that this function returns not-null only for BI and delegation inference
        if (mode != Mode.DelegatedPropertyCompletion && !usedOuterCs) return null

        val fir = fir
        if (fir !is FirCallableDeclaration) return null

        val dispatchReceiverType = fir.dispatchReceiverType ?: return null
        val updatedDispatchReceiverType = finalSubstitutor.substituteOrNull(dispatchReceiverType) ?: return null

        val scope =
            updatedDispatchReceiverType.scope(
                session,
                scopeSession,
                CallableCopyTypeCalculator.DoNothing,
                FirResolvePhase.STATUS
            ) as? FirClassSubstitutionScope ?: return null

        val original = fir.originalForSubstitutionOverride ?: return null

        if (fir is FirSyntheticProperty && fir.symbol is FirSimpleSyntheticPropertySymbol && original is FirSyntheticProperty) {
            var result: FirBasedSymbol<*>? = null
            FirSyntheticPropertiesScope.createIfSyntheticNamesProviderIsDefined(session, updatedDispatchReceiverType, scope)
                ?.processPropertiesByName(fir.name) {
                    val newProperty = it.fir as? FirSyntheticProperty ?: return@processPropertiesByName
                    val originalForNew = newProperty.originalForSubstitutionOverride ?: return@processPropertiesByName
                    if (originalForNew.getter.delegate == original.getter.delegate) {
                        check(result == null)
                        result = it
                    }
                }

            return result ?: error("Not found synthetic property: ${fir.renderWithType()}")
        }

        return findSingleSubstitutedSymbolWithOriginal(original.symbol) { processor ->
            when (original) {
                is FirSimpleFunction -> scope.processFunctionsByName(original.name, processor)
                is FirProperty -> scope.processPropertiesByName(original.name, processor)
                is FirConstructor -> scope.processDeclaredConstructors(processor)
                else -> error("Unexpected declaration kind ${original.render()}")
            }
        }
    }

    private fun findSingleSubstitutedSymbolWithOriginal(
        original: FirBasedSymbol<*>,
        processCallables: ((FirCallableSymbol<*>) -> Unit) -> Unit,
    ): FirBasedSymbol<*> {
        var result: FirBasedSymbol<*>? = null

        processCallables { symbol ->
            if (symbol.originalForSubstitutionOverride == original) {
                check(result == null) {
                    "Expected single, but ${result!!.fir.render()} and ${symbol.fir.render()} found"
                }
                result = symbol
            }
        }

        return result ?: error("No symbol found for ${original.fir.render()}")
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: ExpectedArgumentType?,
    ): FirStatement {
        val calleeReference = qualifiedAccessExpression.calleeReference as? FirNamedReferenceWithCandidate
            ?: return qualifiedAccessExpression
        val result = prepareQualifiedTransform(qualifiedAccessExpression, calleeReference)
        val subCandidate = calleeReference.candidate

        val resultType = result.resolvedType.substituteType(subCandidate)
        resultType.ensureResolvedTypeDeclaration(session)
        result.replaceConeTypeOrNull(resultType)
        session.lookupTracker?.recordTypeResolveAsLookup(resultType, qualifiedAccessExpression.source, context.file.source)

        result.addNonFatalDiagnostics(calleeReference)
        return result
    }

    override fun transformPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: ExpectedArgumentType?,
    ): FirStatement {
        return transformQualifiedAccessExpression(propertyAccessExpression, data)
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: ExpectedArgumentType?): FirStatement {
        val calleeReference = functionCall.calleeReference as? FirNamedReferenceWithCandidate
            ?: return functionCall
        val result = prepareQualifiedTransform(functionCall, calleeReference)
        val subCandidate = calleeReference.candidate
        val resultType = result.resolvedType.substituteType(subCandidate)
        if (calleeReference.isError) {
            subCandidate.argumentMapping?.let {
                result.replaceArgumentList(buildArgumentListForErrorCall(result.argumentList, it))
            }
        } else {
            subCandidate.handleVarargs()
            subCandidate.argumentMapping?.let {
                val newArgumentList = buildResolvedArgumentList(it, source = functionCall.argumentList.source)
                val symbol = subCandidate.symbol
                val functionIsInline =
                    (symbol as? FirNamedFunctionSymbol)?.fir?.isInline == true || symbol.isArrayConstructorWithLambda
                for ((argument, parameter) in newArgumentList.mapping) {
                    val lambda = (argument.unwrapArgument() as? FirAnonymousFunctionExpression)?.anonymousFunction ?: continue
                    val parameterIsSomeFunction = parameter.returnTypeRef.coneType.isSomeFunctionType(session)
                    val inlineStatus = when {
                        !parameterIsSomeFunction -> InlineStatus.NoInline
                        parameter.isCrossinline && functionIsInline -> InlineStatus.CrossInline
                        parameter.isNoinline -> InlineStatus.NoInline
                        functionIsInline -> InlineStatus.Inline
                        else -> InlineStatus.NoInline
                    }
                    lambda.replaceInlineStatus(inlineStatus)
                }
                result.replaceArgumentList(newArgumentList)
            }
        }
        val expectedArgumentsTypeMapping = runIf(!calleeReference.isError) { subCandidate.createArgumentsMapping() }

        result.transformWithExpectedTypes(expectedArgumentsTypeMapping)

        result.replaceConeTypeOrNull(resultType)
        session.lookupTracker?.recordTypeResolveAsLookup(resultType, functionCall.source, context.file.source)

        if (enableArrayOfCallTransformation) {
            return arrayOfCallTransformer.transformFunctionCall(result, session)
        }

        result.addNonFatalDiagnostics(calleeReference)
        return result
    }

    private fun FirQualifiedAccessExpression.addNonFatalDiagnostics(calleeReference: FirNamedReferenceWithCandidate) {
        if (calleeReference.candidate.doesResolutionResultOverrideOtherToPreserveCompatibility()) {
            addNonFatalDiagnostic(ConeResolutionResultOverridesOtherToPreserveCompatibility)
        }

        if (CallToDeprecatedOverrideOfHidden in calleeReference.candidate.diagnostics) {
            addNonFatalDiagnostic(ConeCallToDeprecatedOverrideOfHidden)
        }
    }

    private fun FirCall.transformWithExpectedTypes(expectedArgumentsTypeMapping: ExpectedArgumentType.ArgumentsMap?) {
        class SamConversionInsertionTransformer : FirTransformer<Nothing?>() {
            override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
                // We want to handle only the most top-level "real" expressions
                // We only recursively transform named, spread, lambda argument and vararg expressions.
                if (element is FirWrappedArgumentExpression || element is FirVarargArgumentsExpression) {
                    @Suppress("UNCHECKED_CAST")
                    return element.transformChildren(this, null) as E
                }

                // Once we encounter the first "real" expression, we delegate to the outer transformer.
                val transformed = element.transformSingle(this@FirCallCompletionResultsWriterTransformer, expectedArgumentsTypeMapping)

                // Finally, the result can be wrapped in a SAM conversion if necessary.
                if (transformed is FirExpression) {
                    val key = (element as? FirAnonymousFunctionExpression)?.anonymousFunction ?: element
                    if (expectedArgumentsTypeMapping?.samFunctionTypes?.get(key) != null) {
                        val expectedArgumentType = expectedArgumentsTypeMapping.getExpectedType(key)
                        @Suppress("UNCHECKED_CAST")
                        expectedArgumentType?.let { return transformed.wrapInSamExpression(it) as E }
                    }
                }

                return transformed
            }
        }

        argumentList.transformArguments(SamConversionInsertionTransformer(), null)
    }

    private fun FirExpression.wrapInSamExpression(expectedArgumentType: ConeKotlinType): FirExpression {
        return buildSamConversionExpression {
            expression = this@wrapInSamExpression
            coneTypeOrNull = expectedArgumentType.withNullability(resolvedType.nullability, session.typeContext)
                .let { typeApproximator.approximateToSuperType(it, TypeApproximatorConfiguration.TypeArgumentApproximation) ?: it }
            source = this@wrapInSamExpression.source?.fakeElement(KtFakeSourceElementKind.SamConversion)
        }
    }

    private val FirBasedSymbol<*>.isArrayConstructorWithLambda: Boolean
        get() {
            val constructor = (this as? FirConstructorSymbol)?.fir ?: return false
            if (constructor.valueParameters.size != 2) return false
            return constructor.returnTypeRef.coneType.isArrayOrPrimitiveArray
        }

    override fun transformAnnotationCall(
        annotationCall: FirAnnotationCall,
        data: ExpectedArgumentType?,
    ): FirStatement {
        val calleeReference = annotationCall.calleeReference as? FirNamedReferenceWithCandidate ?: return annotationCall
        annotationCall.replaceCalleeReference(calleeReference.toResolvedReference())
        val subCandidate = calleeReference.candidate
        val expectedArgumentsTypeMapping = runIf(!calleeReference.isError) { subCandidate.createArgumentsMapping() }
        withFirArrayOfCallTransformer {
            annotationCall.argumentList.transformArguments(this, expectedArgumentsTypeMapping)
            var index = 0
            subCandidate.argumentMapping = subCandidate.argumentMapping?.let {
                LinkedHashMap<FirExpression, FirValueParameter>(it.size).let { newMapping ->
                    subCandidate.argumentMapping?.mapKeysTo(newMapping) { (_, _) ->
                        annotationCall.argumentList.arguments[index++]
                    }
                }
            }
        }
        if (calleeReference.isError) {
            subCandidate.argumentMapping?.let {
                annotationCall.replaceArgumentList(buildArgumentListForErrorCall(annotationCall.argumentList, it))
            }
        } else {
            subCandidate.handleVarargs()
            subCandidate.argumentMapping?.let {
                annotationCall.replaceArgumentList(buildResolvedArgumentList(it, annotationCall.argumentList.source))
            }
        }
        return annotationCall
    }

    override fun transformErrorAnnotationCall(errorAnnotationCall: FirErrorAnnotationCall, data: ExpectedArgumentType?): FirStatement {
        return transformAnnotationCall(errorAnnotationCall, data)
    }

    private fun Candidate.handleVarargs() {
        val argumentMapping = this.argumentMapping
        val varargParameter = argumentMapping?.values?.firstOrNull { it.isVararg }
        if (varargParameter != null) {
            // Create a FirVarargArgumentExpression for the vararg arguments
            val varargParameterTypeRef = varargParameter.returnTypeRef
            val resolvedArrayType = varargParameterTypeRef.substitute(this)
            this.argumentMapping = remapArgumentsWithVararg(varargParameter, resolvedArrayType, argumentMapping)
        }
    }

    private fun <D : FirExpression> D.replaceTypeWithSubstituted(
        calleeReference: FirNamedReferenceWithCandidate,
        typeRef: FirResolvedTypeRef,
    ): D {
        val resultType = typeRef.type.substituteType(calleeReference.candidate)
        replaceConeTypeOrNull(resultType)
        session.lookupTracker?.recordTypeResolveAsLookup(resultType, source, context.file.source)
        return this
    }

    private fun ConeKotlinType.substituteType(
        candidate: Candidate,
    ): ConeKotlinType {
        val initialType = candidate.substitutor.substituteOrSelf(type)
        val substitutedType = finallySubstituteOrNull(initialType)
        val finalType = typeApproximator.approximateToSuperType(
            type = substitutedType ?: initialType,
            TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2,
        ) ?: substitutedType

        // This is probably a temporary hack, but it seems necessary because elvis has that attribute and it may leak further like
        // fun <E> foo() = materializeNullable<E>() ?: materialize<E>() // `foo` return type unexpectedly gets inferred to @Exact E
        //
        // In FE1.0, it's not necessary since the annotation for elvis have some strange form (see org.jetbrains.kotlin.resolve.descriptorUtil.AnnotationsWithOnly)
        // that is not propagated further.
        return finalType?.removeExactAttribute() ?: this
    }

    private fun ConeKotlinType.removeExactAttribute(): ConeKotlinType {
        if (attributes.contains(CompilerConeAttributes.Exact)) {
            return withAttributes(attributes.remove(CompilerConeAttributes.Exact))
        }

        return this
    }

    override fun transformSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: ExpectedArgumentType?,
    ): FirStatement {
        safeCallExpression.transformSelector(
            this,
            data?.getExpectedType(
                safeCallExpression
            )?.toExpectedType()
        )

        safeCallExpression.propagateTypeFromQualifiedAccessAfterNullCheck(session, context.file)

        return safeCallExpression
    }

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: ExpectedArgumentType?,
    ): FirStatement {
        val calleeReference =
            callableReferenceAccess.calleeReference as? FirNamedReferenceWithCandidate ?: return callableReferenceAccess
        val subCandidate = calleeReference.candidate
        val typeArguments = computeTypeArguments(callableReferenceAccess, subCandidate)

        val initialType = calleeReference.candidate.substitutor.substituteOrSelf(callableReferenceAccess.resolvedType)
        val finalType = finallySubstituteOrSelf(initialType)

        callableReferenceAccess.replaceConeTypeOrNull(finalType)
        callableReferenceAccess.replaceTypeArguments(typeArguments)
        session.lookupTracker?.recordTypeResolveAsLookup(
            finalType,
            callableReferenceAccess.source ?: callableReferenceAccess.source,
            context.file.source
        )

        val resolvedReference = when (calleeReference) {
            is FirErrorReferenceWithCandidate -> calleeReference.toErrorReference(calleeReference.diagnostic)
            else -> buildResolvedCallableReference {
                source = calleeReference.source
                name = calleeReference.name
                resolvedSymbol = calleeReference.candidateSymbol
                inferredTypeArguments.addAll(computeTypeArgumentTypes(calleeReference.candidate))
                mappedArguments = subCandidate.callableReferenceAdaptation?.mappedArguments ?: emptyMap()
            }
        }

        var dispatchReceiver = subCandidate.dispatchReceiverExpression()
        var extensionReceiver = subCandidate.chosenExtensionReceiverExpression()

        if (subCandidate.usedOuterCs) {
            val updaterForThisReferences = TypeUpdaterForPCLAAndDelegateReceivers()
            dispatchReceiver = dispatchReceiver?.transformSingle(updaterForThisReferences, null)
            extensionReceiver = extensionReceiver?.transformSingle(updaterForThisReferences, null)
        }

        return callableReferenceAccess.apply {
            replaceCalleeReference(resolvedReference)
            replaceDispatchReceiver(dispatchReceiver)
            replaceExtensionReceiver(extensionReceiver)
            addNonFatalDiagnostics(calleeReference)
        }
    }

    override fun transformSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: ExpectedArgumentType?): FirStatement {
        return smartCastExpression.transformOriginalExpression(this, data)
    }

    private inner class TypeUpdaterForPCLAAndDelegateReceivers : FirTransformer<Any?>() {
        override fun <E : FirElement> transformElement(element: E, data: Any?): E {
            return element
        }

        override fun transformThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: Any?): FirStatement {
            return transformTypeRefForQualifiedAccess(thisReceiverExpression)
        }

        override fun transformQualifiedAccessExpression(
            qualifiedAccessExpression: FirQualifiedAccessExpression,
            data: Any?,
        ): FirStatement {
            return transformTypeRefForQualifiedAccess(qualifiedAccessExpression)
        }

        override fun transformPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: Any?): FirStatement {
            return transformQualifiedAccessExpression(propertyAccessExpression, data)
        }

        private fun transformTypeRefForQualifiedAccess(qualifiedAccessExpression: FirQualifiedAccessExpression): FirQualifiedAccessExpression {
            val originalType = qualifiedAccessExpression.resolvedType
            val substitutedReceiverType = finallySubstituteOrNull(originalType) ?: return qualifiedAccessExpression
            qualifiedAccessExpression.replaceConeTypeOrNull(substitutedReceiverType)
            session.lookupTracker?.recordTypeResolveAsLookup(substitutedReceiverType, qualifiedAccessExpression.source, context.file.source)
            return qualifiedAccessExpression
        }
    }

    private fun FirTypeRef.substitute(candidate: Candidate): ConeKotlinType {
        return coneType.substitute(candidate)
    }

    private fun ConeKotlinType.substitute(candidate: Candidate): ConeKotlinType {
        return finallySubstituteOrSelf(candidate.substitutor.substituteOrSelf(this))
    }

    private fun Candidate.createArgumentsMapping(): ExpectedArgumentType.ArgumentsMap? {
        val lambdasReturnType = postponedAtoms.filterIsInstance<ResolvedLambdaAtom>().associate {
            Pair(it.atom, finallySubstituteOrSelf(substitutor.substituteOrSelf(it.returnType)))
        }

        val isIntegerOperator = symbol.isWrappedIntegerOperator()

        var samConversions: MutableMap<FirElement, ConeKotlinType>? = null
        val arguments = argumentMapping?.flatMap { (argument, valueParameter) ->
            val expectedType = when {
                isIntegerOperator -> ConeIntegerConstantOperatorTypeImpl(
                    isUnsigned = symbol.isWrappedIntegerOperatorForUnsignedType() && callInfo.name in binaryOperatorsWithSignedArgument,
                    ConeNullability.NOT_NULL
                )
                valueParameter.isVararg -> valueParameter.returnTypeRef.substitute(this).varargElementType()
                else -> valueParameter.returnTypeRef.substitute(this)
            }

            argument.unwrapAndFlattenArgument(flattenArrays = false).map {
                val element: FirElement = (it as? FirAnonymousFunctionExpression)?.anonymousFunction ?: it
                val samFunctionType = functionTypesOfSamConversions?.get(it)
                if (samFunctionType != null) {
                    if (samConversions == null) samConversions = mutableMapOf()
                    samConversions!![element] = samFunctionType.substitute(this)
                }
                element to expectedType
            }
        }?.toMap()

        if (lambdasReturnType.isEmpty() && arguments.isNullOrEmpty()) return null
        return ExpectedArgumentType.ArgumentsMap(arguments ?: emptyMap(), lambdasReturnType, samConversions ?: emptyMap())
    }

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: ExpectedArgumentType?,
    ): FirStatement {
        val calleeReference =
            delegatedConstructorCall.calleeReference as? FirNamedReferenceWithCandidate ?: return delegatedConstructorCall
        val subCandidate = calleeReference.candidate

        if (calleeReference.isError) {
            subCandidate.argumentMapping?.let {
                delegatedConstructorCall.replaceArgumentList(buildArgumentListForErrorCall(delegatedConstructorCall.argumentList, it))
            }
        } else {
            subCandidate.handleVarargs()
            subCandidate.argumentMapping?.let {
                delegatedConstructorCall.replaceArgumentList(buildResolvedArgumentList(it, delegatedConstructorCall.argumentList.source))
            }
        }

        runPCLARelatedTasksForCandidate(subCandidate)

        val argumentsMapping = runIf(!calleeReference.isError) { subCandidate.createArgumentsMapping() }
        delegatedConstructorCall.transformWithExpectedTypes(argumentsMapping)

        return delegatedConstructorCall.apply {
            replaceCalleeReference(calleeReference.toResolvedReference())
        }
    }

    private fun computeTypeArguments(
        access: FirQualifiedAccessExpression,
        candidate: Candidate,
    ): List<FirTypeProjection> {
        val typeArguments = computeTypeArgumentTypes(candidate)
            .mapIndexed { index, type ->
                when (val argument = access.typeArguments.getOrNull(index)) {
                    is FirTypeProjectionWithVariance -> {
                        val typeRef = argument.typeRef as FirResolvedTypeRef
                        buildTypeProjectionWithVariance {
                            source = argument.source
                            this.typeRef = if (typeRef.type is ConeErrorType) typeRef else typeRef.withReplacedConeType(type)
                            variance = argument.variance
                        }
                    }
                    is FirStarProjection -> {
                        buildStarProjection {
                            source = argument.source
                        }
                    }
                    else -> {
                        buildTypeProjectionWithVariance {
                            source = argument?.source
                            typeRef = type.toFirResolvedTypeRef()
                            variance = Variance.INVARIANT
                        }
                    }
                }
            }

        // We must ensure that all extra type arguments are preserved in the result, so that they can still be resolved later (e.g. for
        // navigation in the IDE).
        return if (typeArguments.size < access.typeArguments.size) {
            typeArguments + access.typeArguments.subList(typeArguments.size, access.typeArguments.size)
        } else typeArguments
    }

    private fun computeTypeArgumentTypes(
        candidate: Candidate,
    ): List<ConeKotlinType> {
        val declaration = candidate.symbol.fir as? FirCallableDeclaration ?: return emptyList()

        return declaration.typeParameters.map {
            val typeParameter = ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false)
            val substitution = candidate.substitutor.substituteOrSelf(typeParameter)
            finallySubstituteOrSelf(substitution).let { substitutedType ->
                typeApproximator.approximateToSuperType(
                    substitutedType, TypeApproximatorConfiguration.TypeArgumentApproximation,
                ) ?: substitutedType
            }
        }
    }

    override fun transformAnonymousFunctionExpression(
        anonymousFunctionExpression: FirAnonymousFunctionExpression,
        data: ExpectedArgumentType?,
    ): FirStatement {
        return anonymousFunctionExpression.transformAnonymousFunction(this, data)
    }

    override fun transformAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        data: ExpectedArgumentType?,
    ): FirStatement {
        // The case where we can't find any return expressions not common, and happens when there are anonymous function arguments
        // that aren't mapped to any parameter in the call. So, we don't run body resolve transformation for them, thus there's
        // no control flow info either. Example: second lambda in the call like list.filter({}, {})
        val returnExpressions = dataFlowAnalyzer.returnExpressionsOfAnonymousFunctionOrNull(anonymousFunction)?.map { it.expression }
            ?: return transformImplicitTypeRefInAnonymousFunction(anonymousFunction)

        val expectedType = data?.getExpectedType(anonymousFunction)?.let { expectedArgumentType ->
            // From the argument mapping, the expected type of this anonymous function would be:
            when {
                // a built-in functional type, no-brainer
                expectedArgumentType.isSomeFunctionType(session) -> expectedArgumentType
                // fun interface (a.k.a. SAM), then unwrap it and build a functional type from that interface function
                else -> {
                    val samFunctionType = (data as? ExpectedArgumentType.ArgumentsMap)?.samFunctionTypes?.get(anonymousFunction)
                        ?: samResolver.getFunctionTypeForPossibleSamType(expectedArgumentType)
                    samFunctionType?.lowerBoundIfFlexible()
                }
            }
        }

        var needUpdateLambdaType = anonymousFunction.typeRef is FirImplicitTypeRef

        val receiverParameter = anonymousFunction.receiverParameter
        val initialReceiverType = receiverParameter?.typeRef?.coneTypeSafe<ConeKotlinType>()
        val resultReceiverType = initialReceiverType?.let { finallySubstituteOrNull(it) }
        if (resultReceiverType != null) {
            receiverParameter.replaceTypeRef(receiverParameter.typeRef.resolvedTypeFromPrototype(resultReceiverType))
            needUpdateLambdaType = true
        }

        val initialReturnType = anonymousFunction.returnTypeRef.coneTypeSafe<ConeKotlinType>()
        val expectedReturnType = initialReturnType?.let { finallySubstituteOrSelf(it) }
            ?: runIf(returnExpressions.any { it.source?.kind is KtFakeSourceElementKind.ImplicitUnit.Return })
            { session.builtinTypes.unitType.coneType }
            ?: expectedType?.returnType(session) as? ConeClassLikeType
            ?: (data as? ExpectedArgumentType.ArgumentsMap)?.lambdasReturnTypes?.get(anonymousFunction)

        val newData = expectedReturnType?.toExpectedType()
        val result = transformElement(anonymousFunction, newData)
        for (expression in returnExpressions) {
            expression.transformSingle(this, newData)
        }

        // Prefer the expected type over the inferred one - the latter is a subtype of the former in valid code,
        // and there will be ARGUMENT_TYPE_MISMATCH errors on the lambda's return expressions in invalid code.
        val resultReturnType = expectedReturnType
            ?: session.typeContext.commonSuperTypeOrNull(returnExpressions.map { it.resolvedType })
            ?: session.builtinTypes.unitType.type

        if (initialReturnType != resultReturnType) {
            result.replaceReturnTypeRef(result.returnTypeRef.resolvedTypeFromPrototype(resultReturnType))
            session.lookupTracker?.recordTypeResolveAsLookup(result.returnTypeRef, result.source, context.file.source)
            needUpdateLambdaType = true
        }

        if (needUpdateLambdaType) {
            val kind = expectedType?.functionTypeKind(session)
                ?: result.typeRef.coneTypeSafe<ConeClassLikeType>()?.functionTypeKind(session)
            result.replaceTypeRef(result.constructFunctionTypeRef(session, kind))
            session.lookupTracker?.recordTypeResolveAsLookup(result.typeRef, result.source, context.file.source)
        }
        // Have to delay this until the type is written to avoid adding a return if the type is Unit.
        result.addReturnToLastStatementIfNeeded(session)
        return result
    }

    private fun transformImplicitTypeRefInAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
    ): FirStatement {
        val implicitTypeTransformer = object : FirDefaultTransformer<Any?>() {
            override fun <E : FirElement> transformElement(element: E, data: Any?): E {
                @Suppress("UNCHECKED_CAST")
                return (element.transformChildren(this, data) as E)
            }

            override fun transformImplicitTypeRef(
                implicitTypeRef: FirImplicitTypeRef,
                data: Any?,
            ): FirTypeRef =
                buildErrorTypeRef {
                    source = implicitTypeRef.source
                    // NB: this error message assumes that it is used only if CFG for the anonymous function is not available
                    diagnostic = ConeSimpleDiagnostic("Cannot infer type w/o CFG", DiagnosticKind.InferenceError)
                }

        }
        // NB: if we transform simply all children, there would be too many type error reports.
        anonymousFunction.transformReturnTypeRef(implicitTypeTransformer, null)
        anonymousFunction.transformValueParameters(implicitTypeTransformer, null)
        anonymousFunction.transformBody(implicitTypeTransformer, null)
        return anonymousFunction
    }

    override fun transformReturnExpression(
        returnExpression: FirReturnExpression,
        data: ExpectedArgumentType?,
    ): FirStatement {
        val labeledElement = returnExpression.target.labeledElement
        if (labeledElement is FirAnonymousFunction) {
            return returnExpression
        }

        val newData = labeledElement.returnTypeRef.coneTypeSafe<ConeKotlinType>()?.toExpectedType()
        return super.transformReturnExpression(returnExpression, newData)
    }

    override fun transformBlock(block: FirBlock, data: ExpectedArgumentType?): FirStatement {
        val initialType = block.resolvedType
        var resultType = finallySubstituteOrNull(initialType) ?: block.resolvedType
        (resultType as? ConeIntegerLiteralType)?.let {
            resultType =
                it.getApproximatedType(data?.getExpectedType(block)?.fullyExpandedType(session))
        }
        block.replaceConeTypeOrNull(resultType)
        session.lookupTracker?.recordTypeResolveAsLookup(resultType, block.source, context.file.source)
        transformElement(block, data)
        if (block.resolvedType is ConeErrorType) {
            block.writeResultType(session)
        }
        return block
    }

    // Transformations for synthetic calls generated by FirSyntheticCallGenerator

    override fun transformWhenExpression(
        whenExpression: FirWhenExpression,
        data: ExpectedArgumentType?,
    ): FirStatement {
        return transformSyntheticCall(whenExpression, data)
    }

    override fun transformTryExpression(
        tryExpression: FirTryExpression,
        data: ExpectedArgumentType?,
    ): FirStatement {
        return transformSyntheticCall(tryExpression, data)
    }

    override fun transformCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: ExpectedArgumentType?,
    ): FirStatement {
        return transformSyntheticCall(checkNotNullCall, data)
    }

    override fun transformElvisExpression(
        elvisExpression: FirElvisExpression,
        data: ExpectedArgumentType?,
    ): FirStatement {
        return transformSyntheticCall(elvisExpression, data)
    }

    private inline fun <reified D> transformSyntheticCall(
        syntheticCall: D,
        data: ExpectedArgumentType?,
    ): FirStatement where D : FirResolvable, D : FirExpression {
        val calleeReference = syntheticCall.calleeReference as? FirNamedReferenceWithCandidate
        val declaration = calleeReference?.candidate?.symbol?.fir as? FirSimpleFunction

        if (calleeReference == null || declaration == null) {
            transformSyntheticCallChildren(syntheticCall, data)
            return syntheticCall
        }

        val typeRef = typeCalculator.tryCalculateReturnType(declaration)
        syntheticCall.replaceTypeWithSubstituted(calleeReference, typeRef)
        transformSyntheticCallChildren(syntheticCall, data)

        runPCLARelatedTasksForCandidate(calleeReference.candidate)

        return syntheticCall.apply {
            replaceCalleeReference(calleeReference.toResolvedReference())
        }
    }

    private inline fun <reified D> transformSyntheticCallChildren(
        syntheticCall: D,
        data: ExpectedArgumentType?,
    ) where D : FirResolvable, D : FirExpression {
        val newData = data?.getExpectedType(syntheticCall)?.toExpectedType() ?: syntheticCall.resolvedType.toExpectedType()

        if (syntheticCall is FirTryExpression) {
            syntheticCall.transformCalleeReference(this, newData)
            syntheticCall.transformTryBlock(this, newData)
            syntheticCall.transformCatches(this, newData)
            return
        }

        syntheticCall.transformChildren(
            this,
            data = newData
        )
    }

    override fun <T> transformLiteralExpression(
        literalExpression: FirLiteralExpression<T>,
        data: ExpectedArgumentType?,
    ): FirStatement {
        if (data == ExpectedArgumentType.NoApproximation) return literalExpression
        val expectedType = data?.getExpectedType(literalExpression)
        if (expectedType is ConeIntegerConstantOperatorType) {
            return literalExpression
        }
        return literalExpression.transformSingle(integerOperatorApproximator, expectedType)
    }

    override fun transformIntegerLiteralOperatorCall(
        integerLiteralOperatorCall: FirIntegerLiteralOperatorCall,
        data: ExpectedArgumentType?,
    ): FirStatement {
        if (data == ExpectedArgumentType.NoApproximation) return integerLiteralOperatorCall
        val expectedType = data?.getExpectedType(integerLiteralOperatorCall)
        if (expectedType is ConeIntegerConstantOperatorType) {
            return integerLiteralOperatorCall
        }
        return integerLiteralOperatorCall.transformSingle(integerOperatorApproximator, expectedType)
    }

    override fun transformArrayLiteral(arrayLiteral: FirArrayLiteral, data: ExpectedArgumentType?): FirStatement {
        if (arrayLiteral.isResolved) return arrayLiteral
        val expectedArrayType = data?.getExpectedType(arrayLiteral)
        val expectedArrayElementType = expectedArrayType?.arrayElementType()
        arrayLiteral.transformChildren(this, expectedArrayElementType?.toExpectedType())
        val arrayElementType =
            session.typeContext.commonSuperTypeOrNull(arrayLiteral.arguments.map { it.resolvedType })?.let {
                typeApproximator.approximateToSuperType(it, TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2)
                    ?: it
            } ?: expectedArrayElementType ?: session.builtinTypes.nullableAnyType.type
        arrayLiteral.resultType =
            arrayElementType.createArrayType(createPrimitiveArrayTypeIfPossible = expectedArrayType?.isPrimitiveArray == true)
        return arrayLiteral
    }

    override fun transformVarargArgumentsExpression(
        varargArgumentsExpression: FirVarargArgumentsExpression,
        data: ExpectedArgumentType?,
    ): FirStatement {
        val expectedType = data?.getExpectedType(varargArgumentsExpression)?.let { ExpectedArgumentType.ExpectedType(it) }
        varargArgumentsExpression.transformChildren(this, expectedType)
        return varargArgumentsExpression
    }

    // TODO: report warning with a checker and return true here only in case of errors, KT-59676
    private fun FirNamedReferenceWithCandidate.hasAdditionalResolutionErrors(): Boolean =
        candidate.system.errors.any { it is InferredEmptyIntersection }

    private fun FirNamedReferenceWithCandidate.toResolvedReference(): FirNamedReference {
        val errorDiagnostic = when {
            this is FirErrorReferenceWithCandidate -> this.diagnostic
            !candidate.currentApplicability.isSuccess -> ConeInapplicableCandidateError(candidate.currentApplicability, candidate)
            !candidate.isSuccessful -> {
                require(candidate.system.hasContradiction) {
                    "Candidate is not successful, but system has no contradiction"
                }

                ConeConstraintSystemHasContradiction(candidate)
            }
            // NB: these additional errors might not lead to marking candidate unsuccessful because it may be a warning in FE 1.0
            // We consider those warnings as errors in FIR
            hasAdditionalResolutionErrors() -> ConeConstraintSystemHasContradiction(candidate)
            else -> null
        }

        return when (errorDiagnostic) {
            null -> buildResolvedNamedReference {
                source = this@toResolvedReference.source
                name = this@toResolvedReference.name
                resolvedSymbol = this@toResolvedReference.candidateSymbol
            }

            else -> toErrorReference(errorDiagnostic)
        }
    }
}

sealed class ExpectedArgumentType {
    class ArgumentsMap(
        val map: Map<FirElement, ConeKotlinType>,
        val lambdasReturnTypes: Map<FirAnonymousFunction, ConeKotlinType>,
        val samFunctionTypes: Map<FirElement, ConeKotlinType>,
    ) : ExpectedArgumentType()

    class ExpectedType(val type: ConeKotlinType) : ExpectedArgumentType()
    object NoApproximation : ExpectedArgumentType()
}

private fun ExpectedArgumentType.getExpectedType(argument: FirElement): ConeKotlinType? = when (this) {
    is ExpectedArgumentType.ArgumentsMap -> map[argument]
    is ExpectedArgumentType.ExpectedType -> type
    ExpectedArgumentType.NoApproximation -> null
}

fun ConeKotlinType.toExpectedType(): ExpectedArgumentType = ExpectedArgumentType.ExpectedType(this)

internal fun Candidate.doesResolutionResultOverrideOtherToPreserveCompatibility(): Boolean =
    ResolutionResultOverridesOtherToPreserveCompatibility in diagnostics

internal fun FirQualifiedAccessExpression.addNonFatalDiagnostic(diagnostic: ConeDiagnostic) {
    replaceNonFatalDiagnostics(nonFatalDiagnostics + listOf(diagnostic))
}
