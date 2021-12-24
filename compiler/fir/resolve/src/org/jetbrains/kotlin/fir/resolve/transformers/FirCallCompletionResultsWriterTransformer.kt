/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirPropertyAccessExpressionImpl
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedCallableReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.FirErrorReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.dfa.FirDataFlowAnalyzer
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeConstraintSystemHasContradiction
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConePropertyAsOperator
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeTypeParameterInQualifiedAccess
import org.jetbrains.kotlin.fir.resolve.inference.ResolvedLambdaAtom
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirArrayOfCallTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.remapArgumentsWithVararg
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.writeResultType
import org.jetbrains.kotlin.fir.scopes.impl.isWrappedIntegerOperator
import org.jetbrains.kotlin.fir.scopes.impl.isWrappedIntegerOperatorForUnsignedType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.collections.component1
import kotlin.collections.component2

class FirCallCompletionResultsWriterTransformer(
    override val session: FirSession,
    private val finalSubstitutor: ConeSubstitutor,
    private val typeCalculator: ReturnTypeCalculator,
    private val typeApproximator: ConeTypeApproximator,
    private val dataFlowAnalyzer: FirDataFlowAnalyzer<*>,
    private val integerOperatorApproximator: IntegerLiteralAndOperatorApproximationTransformer,
    private val context: BodyResolveContext,
    private val mode: Mode = Mode.Normal
) : FirAbstractTreeTransformer<ExpectedArgumentType?>(phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {

    private val declarationWriter by lazy { FirDeclarationCompletionResultsWriter(finalSubstitutor, typeApproximator, session.typeContext) }

    private val arrayOfCallTransformer = FirArrayOfCallTransformer()
    private var enableArrayOfCallTransformation = false

    private val samResolver: FirSamResolver = FirSamResolverImpl(session, ScopeSession())

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

    private fun <T : FirQualifiedAccess> prepareQualifiedTransform(
        qualifiedAccessExpression: T, calleeReference: FirNamedReferenceWithCandidate
    ): T {
        val subCandidate = calleeReference.candidate
        val declaration = subCandidate.symbol.fir
        val typeArguments = computeTypeArguments(qualifiedAccessExpression, subCandidate)
        val typeRef = if (declaration is FirTypedDeclaration) {
            val calculated = typeCalculator.tryCalculateReturnType(declaration)
            if (calculated !is FirErrorTypeRef) {
                buildResolvedTypeRef {
                    source = calculated.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef)
                    annotations += calculated.annotations
                    type = calculated.type
                }
            } else {
                buildErrorTypeRef {
                    source = calculated.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef)
                    type = calculated.type
                    diagnostic = calculated.diagnostic
                }
            }
        } else {
            // this branch is for cases when we have
            // some invalid qualified access expression itself.
            // e.g. `T::toString` where T is a generic type.
            // in these cases we should report an error on
            // the calleeReference.source which is not a fake source.
            buildErrorTypeRef {
                source = calleeReference.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef)
                diagnostic =
                    when (declaration) {
                        is FirTypeParameter -> ConeTypeParameterInQualifiedAccess(declaration.symbol)
                        else -> ConeSimpleDiagnostic("Callee reference to candidate without return type: ${declaration.render()}")
                    }
            }
        }

        var dispatchReceiver = subCandidate.dispatchReceiverExpression()
        var extensionReceiver = subCandidate.extensionReceiverExpression()
        if (!declaration.isWrappedIntegerOperator()) {
            val expectedDispatchReceiverType = (declaration as? FirCallableDeclaration)?.dispatchReceiverType
            val expectedExtensionReceiverType = (declaration as? FirCallableDeclaration)?.receiverTypeRef?.coneType
            dispatchReceiver = dispatchReceiver.transformSingle(integerOperatorApproximator, expectedDispatchReceiverType)
            extensionReceiver = extensionReceiver.transformSingle(integerOperatorApproximator, expectedExtensionReceiverType)
        }

        @Suppress("UNCHECKED_CAST")
        val result = qualifiedAccessExpression
            .transformCalleeReference(
                StoreCalleeReference,
                calleeReference.toResolvedReference(),
            )
            .transformDispatchReceiver(StoreReceiver, dispatchReceiver)
            .transformExtensionReceiver(StoreReceiver, extensionReceiver) as T
        if (result is FirPropertyAccessExpressionImpl && calleeReference.candidate.currentApplicability == CandidateApplicability.PROPERTY_AS_OPERATOR) {
            result.nonFatalDiagnostics.add(ConePropertyAsOperator(calleeReference.candidate.symbol as FirPropertySymbol))
        }

        if (result is FirQualifiedAccessExpression) {
            result.replaceTypeRef(typeRef)
        } else if (result is FirVariableAssignment) {
            result.replaceLValueTypeRef(typeRef)
        }

        if (declaration !is FirErrorFunction) {
            result.replaceTypeArguments(typeArguments)
        }
        session.lookupTracker?.recordTypeResolveAsLookup(typeRef, qualifiedAccessExpression.source, context.file.source)
        return result
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: ExpectedArgumentType?,
    ): FirStatement {
        val calleeReference = qualifiedAccessExpression.calleeReference as? FirNamedReferenceWithCandidate
            ?: return run {
                if (mode == Mode.DelegatedPropertyCompletion) {
                    val typeUpdater = TypeUpdaterForDelegateArguments()
                    qualifiedAccessExpression.transformSingle(typeUpdater, null)
                }
                qualifiedAccessExpression
            }
        val result = prepareQualifiedTransform(qualifiedAccessExpression, calleeReference)
        val typeRef = result.typeRef as FirResolvedTypeRef
        val subCandidate = calleeReference.candidate

        val resultType = typeRef.substituteTypeRef(subCandidate)
        resultType.ensureResolvedTypeDeclaration(session)
        result.replaceTypeRef(resultType)
        session.lookupTracker?.recordTypeResolveAsLookup(resultType, qualifiedAccessExpression.source, context.file.source)

        if (mode == Mode.DelegatedPropertyCompletion) {
            subCandidate.symbol.fir.transformSingle(
                declarationWriter,
                FirDeclarationCompletionResultsWriter.ApproximationData.NoApproximation
            )
            val typeUpdater = TypeUpdaterForDelegateArguments()
            result.transformExplicitReceiver(typeUpdater, null)
        }

        return result
    }

    override fun transformPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: ExpectedArgumentType?
    ): FirStatement {
        return transformQualifiedAccessExpression(propertyAccessExpression, data)
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: ExpectedArgumentType?): FirStatement {
        val calleeReference = functionCall.calleeReference as? FirNamedReferenceWithCandidate
            ?: return functionCall
        val result = prepareQualifiedTransform(functionCall, calleeReference)
        val typeRef = result.typeRef as FirResolvedTypeRef
        val subCandidate = calleeReference.candidate
        val resultType: FirTypeRef
        resultType = typeRef.substituteTypeRef(subCandidate)
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
                    val inlineStatus = when {
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
        result.argumentList.transformArguments(this, expectedArgumentsTypeMapping)
        result.replaceTypeRef(resultType)
        session.lookupTracker?.recordTypeResolveAsLookup(resultType, functionCall.source, context.file.source)

        if (mode == Mode.DelegatedPropertyCompletion) {
            subCandidate.symbol.fir.transformSingle(
                declarationWriter,
                FirDeclarationCompletionResultsWriter.ApproximationData.NoApproximation
            )
            val typeUpdater = TypeUpdaterForDelegateArguments()
            result.argumentList.transformArguments(typeUpdater, null)
            result.transformExplicitReceiver(typeUpdater, null)
        }

        if (enableArrayOfCallTransformation) {
            return arrayOfCallTransformer.transformFunctionCall(result, null)
        }

        return result
    }

    private val FirBasedSymbol<*>.isArrayConstructorWithLambda: Boolean
        get() {
            val constructor = (this as? FirConstructorSymbol)?.fir ?: return false
            if (constructor.valueParameters.size != 2) return false
            return constructor.returnTypeRef.coneType.isArrayOrPrimitiveArray
        }

    override fun transformAnnotationCall(
        annotationCall: FirAnnotationCall,
        data: ExpectedArgumentType?
    ): FirStatement {
        val calleeReference = annotationCall.calleeReference as? FirNamedReferenceWithCandidate
            ?: return annotationCall
        annotationCall.transformCalleeReference(
            StoreCalleeReference,
            calleeReference.toResolvedReference(),
        )
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

    private fun <D : FirExpression> D.replaceTypeRefWithSubstituted(
        calleeReference: FirNamedReferenceWithCandidate,
        typeRef: FirResolvedTypeRef,
    ): D {
        val resultTypeRef = typeRef.substituteTypeRef(calleeReference.candidate)
        replaceTypeRef(resultTypeRef)
        session.lookupTracker?.recordTypeResolveAsLookup(resultTypeRef, source, context.file.source)
        return this
    }

    private fun FirResolvedTypeRef.substituteTypeRef(
        candidate: Candidate,
    ): FirResolvedTypeRef {
        val initialType = candidate.substitutor.substituteOrSelf(type)
        val substitutedType = finalSubstitutor.substituteOrNull(initialType)
        val finalType = typeApproximator.approximateToSuperType(
            type = substitutedType ?: initialType, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference,
        ) ?: substitutedType

        // This is probably a temporary hack, but it seems necessary because elvis has that attribute and it may leak further like
        // fun <E> foo() = materializeNullable<E>() ?: materialize<E>() // `foo` return type unexpectedly gets inferred to @Exact E
        //
        // In FE1.0, it's not necessary since the annotation for elvis have some strange form (see org.jetbrains.kotlin.resolve.descriptorUtil.AnnotationsWithOnly)
        // that is not propagated further.
        val withRemovedExactAttribute = finalType?.removeExactAttribute()

        return withReplacedConeType(withRemovedExactAttribute)
    }

    private fun ConeKotlinType.removeExactAttribute(): ConeKotlinType {
        if (attributes.contains(CompilerConeAttributes.Exact)) {
            return withAttributes(attributes.remove(CompilerConeAttributes.Exact))
        }

        return this
    }

    override fun transformSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: ExpectedArgumentType?
    ): FirStatement {
        safeCallExpression.transformSelector(
            this,
            data?.getExpectedType(
                safeCallExpression
            )?.toExpectedType()
        )

        safeCallExpression.propagateTypeFromQualifiedAccessAfterNullCheck(safeCallExpression.receiver, session, context.file)

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

        val typeRef = callableReferenceAccess.typeRef as FirResolvedTypeRef

        val initialType = calleeReference.candidate.substitutor.substituteOrSelf(typeRef.type)
        val finalType = finalSubstitutor.substituteOrSelf(initialType)

        val resultType = typeRef.withReplacedConeType(finalType)
        callableReferenceAccess.replaceTypeRef(resultType)
        callableReferenceAccess.replaceTypeArguments(typeArguments)
        session.lookupTracker?.recordTypeResolveAsLookup(resultType, typeRef.source ?: callableReferenceAccess.source, context.file.source)

        val resolvedReference = when (calleeReference) {
            is FirErrorReferenceWithCandidate -> buildErrorNamedReference {
                source = calleeReference.source
                diagnostic = calleeReference.diagnostic
                candidateSymbol = calleeReference.candidateSymbol
            }
            else -> buildResolvedCallableReference {
                source = calleeReference.source
                name = calleeReference.name
                resolvedSymbol = calleeReference.candidateSymbol
                inferredTypeArguments.addAll(computeTypeArgumentTypes(calleeReference.candidate))
                mappedArguments = subCandidate.callableReferenceAdaptation?.mappedArguments ?: emptyMap()
            }
        }

        return callableReferenceAccess.transformCalleeReference(
            StoreCalleeReference,
            resolvedReference,
        ).transformDispatchReceiver(StoreReceiver, subCandidate.dispatchReceiverExpression())
            .transformExtensionReceiver(StoreReceiver, subCandidate.extensionReceiverExpression())
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: ExpectedArgumentType?,
    ): FirStatement {
        val calleeReference = variableAssignment.calleeReference as? FirNamedReferenceWithCandidate
            ?: return variableAssignment

        // Initialize lValueTypeRef
        val qualifiedTransform = prepareQualifiedTransform(variableAssignment, calleeReference)
        val lValueTypeRef = qualifiedTransform.lValueTypeRef as FirResolvedTypeRef
        val resultLValueType = lValueTypeRef.substituteTypeRef(calleeReference.candidate)
        resultLValueType.ensureResolvedTypeDeclaration(session)
        variableAssignment.replaceLValueTypeRef(resultLValueType)
        session.lookupTracker?.recordTypeResolveAsLookup(resultLValueType, variableAssignment.lValue.source, context.file.source)

        return variableAssignment.transformCalleeReference(
            StoreCalleeReference,
            calleeReference.toResolvedReference(),
        )
    }

    private inner class TypeUpdaterForDelegateArguments : FirTransformer<Any?>() {
        override fun <E : FirElement> transformElement(element: E, data: Any?): E {
            return element
        }

        override fun transformQualifiedAccessExpression(
            qualifiedAccessExpression: FirQualifiedAccessExpression,
            data: Any?
        ): FirStatement {
            val originalType = qualifiedAccessExpression.typeRef.coneType
            val substitutedReceiverType = finalSubstitutor.substituteOrNull(originalType) ?: return qualifiedAccessExpression
            val resolvedTypeRef = qualifiedAccessExpression.typeRef.resolvedTypeFromPrototype(substitutedReceiverType)
            qualifiedAccessExpression.replaceTypeRef(resolvedTypeRef)
            session.lookupTracker?.recordTypeResolveAsLookup(resolvedTypeRef, qualifiedAccessExpression.source, context.file.source)
            return qualifiedAccessExpression
        }

        override fun transformPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: Any?): FirStatement {
            return transformQualifiedAccessExpression(propertyAccessExpression, data)
        }
    }

    private fun FirTypeRef.substitute(candidate: Candidate): ConeKotlinType =
        coneType.let { candidate.substitutor.substituteOrSelf(it) }
            .let { finalSubstitutor.substituteOrSelf(it) }

    private fun Candidate.createArgumentsMapping(): ExpectedArgumentType? {
        val lambdasReturnType = postponedAtoms.filterIsInstance<ResolvedLambdaAtom>().associate {
            Pair(it.atom, finalSubstitutor.substituteOrSelf(substitutor.substituteOrSelf(it.returnType)).approximateIntegerLiteralType())
        }

        val isIntegerOperator = symbol.isWrappedIntegerOperator()

        val arguments = argumentMapping?.map { (argument, valueParameter) ->
            val expectedType = when {
                isIntegerOperator -> ConeIntegerConstantOperatorTypeImpl(
                    isUnsigned = symbol.isWrappedIntegerOperatorForUnsignedType(),
                    ConeNullability.NOT_NULL
                )
                valueParameter.isVararg -> valueParameter.returnTypeRef.substitute(this).varargElementType()
                else -> valueParameter.returnTypeRef.substitute(this)
            }

            val unwrappedArgument: FirElement = when (val unwrappedArgument = argument.unwrapArgument()) {
                is FirAnonymousFunctionExpression -> unwrappedArgument.anonymousFunction
                else -> unwrappedArgument
            }
            unwrappedArgument to expectedType
        }?.toMap()


        if (lambdasReturnType.isEmpty() && arguments.isNullOrEmpty()) return null
        return ExpectedArgumentType.ArgumentsMap(arguments ?: emptyMap(), lambdasReturnType)
    }

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: ExpectedArgumentType?,
    ): FirStatement {
        val calleeReference =
            delegatedConstructorCall.calleeReference as? FirNamedReferenceWithCandidate ?: return delegatedConstructorCall
        val subCandidate = calleeReference.candidate

        val argumentsMapping = runIf(!calleeReference.isError) { calleeReference.candidate.createArgumentsMapping() }
        delegatedConstructorCall.argumentList.transformArguments(this, argumentsMapping)
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
        return delegatedConstructorCall.transformCalleeReference(
            StoreCalleeReference,
            calleeReference.toResolvedReference(),
        )
    }

    private fun computeTypeArguments(
        access: FirQualifiedAccess,
        candidate: Candidate
    ): List<FirTypeProjection> {
        return computeTypeArgumentTypes(candidate)
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
                            typeRef = buildResolvedTypeRef {
                                this.type = type
                            }
                            variance = Variance.INVARIANT
                        }
                    }
                }
            }
    }

    private fun computeTypeArgumentTypes(
        candidate: Candidate,
    ): List<ConeKotlinType> {
        val declaration = candidate.symbol.fir as? FirCallableDeclaration ?: return emptyList()

        return declaration.typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }
            .map { candidate.substitutor.substituteOrSelf(it) }
            .map {
                finalSubstitutor.substituteOrSelf(it).let { substitutedType ->
                    typeApproximator.approximateToSuperType(
                        substitutedType, TypeApproximatorConfiguration.TypeArgumentApproximation,
                    ) ?: substitutedType
                }
            }
    }

    override fun transformAnonymousFunctionExpression(
        anonymousFunctionExpression: FirAnonymousFunctionExpression,
        data: ExpectedArgumentType?
    ): FirStatement {
        return anonymousFunctionExpression.transformAnonymousFunction(this, data)
    }

    override fun transformAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        data: ExpectedArgumentType?,
    ): FirStatement {
        // This case is not common, and happens when there are anonymous function arguments that aren't mapped to any parameter in the call
        // So, we don't run body resolve transformation for them, thus there's no control flow info either
        // Control flow info is necessary prerequisite because we collect return expressions in that function
        //
        // Example: second lambda in the call like list.filter({}, {})
        if (!dataFlowAnalyzer.isThereControlFlowInfoForAnonymousFunction(anonymousFunction)) {
            // But, don't leave implicit type refs behind
            return transformImplicitTypeRefInAnonymousFunction(anonymousFunction)
        }

        val expectedType = data?.getExpectedType(anonymousFunction)?.let { expectedArgumentType ->
            // From the argument mapping, the expected type of this anonymous function would be:
            when {
                // a built-in functional type, no-brainer
                expectedArgumentType.isBuiltinFunctionalType(session) -> expectedArgumentType
                // fun interface (a.k.a. SAM), then unwrap it and build a functional type from that interface function
                expectedArgumentType is ConeClassLikeType -> {
                    val firRegularClass =
                        session.symbolProvider.getClassLikeSymbolByClassId(expectedArgumentType.lookupTag.classId)?.fir as? FirRegularClass

                    firRegularClass?.let {
                        val functionType = samResolver.getFunctionTypeForPossibleSamType(firRegularClass.defaultType())
                        if (functionType != null) {
                            createFunctionalType(
                                functionType.typeArguments.dropLast(1).map { it as ConeKotlinType },
                                null,
                                functionType.typeArguments.last() as ConeKotlinType,
                                functionType.classId?.relativeClassName?.asString()
                                    ?.startsWith(FunctionClassKind.SuspendFunction.classNamePrefix) == true
                            )
                        } else {
                            null
                        }
                    }
                }
                else -> null
            }
        }

        var needUpdateLambdaType = false

        val initialReceiverType = anonymousFunction.receiverTypeRef?.coneTypeSafe<ConeKotlinType>()
        val resultReceiverType = initialReceiverType?.let { finalSubstitutor.substituteOrNull(it) }
        if (resultReceiverType != null) {
            anonymousFunction.replaceReceiverTypeRef(anonymousFunction.receiverTypeRef!!.resolvedTypeFromPrototype(resultReceiverType))
            needUpdateLambdaType = true
        }

        val initialType = anonymousFunction.returnTypeRef.coneTypeSafe<ConeKotlinType>()

        val finalType = if (anonymousFunction.isLambda) {
            expectedType?.returnType(session) as? ConeClassLikeType
                ?: (data as? ExpectedArgumentType.ArgumentsMap)?.lambdasReturnTypes?.get(anonymousFunction)
                ?: initialType?.let(finalSubstitutor::substituteOrSelf)
        } else {
            initialType?.let(finalSubstitutor::substituteOrSelf)
                ?: expectedType?.returnType(session) as? ConeClassLikeType
                ?: (data as? ExpectedArgumentType.ArgumentsMap)?.lambdasReturnTypes?.get(anonymousFunction)
        }

        if (finalType != null) {
            val resultType = anonymousFunction.returnTypeRef.withReplacedConeType(finalType)
            anonymousFunction.transformReturnTypeRef(StoreType, resultType)
            needUpdateLambdaType = true
        }

        if (needUpdateLambdaType) {
            val resolvedTypeRef =
                anonymousFunction.constructFunctionalTypeRef(
                    isSuspend = expectedType?.isSuspendFunctionType(session) == true ||
                            (expectedType == null && anonymousFunction.isSuspendFunctionType())
                )
            anonymousFunction.replaceTypeRef(resolvedTypeRef)
            session.lookupTracker?.recordTypeResolveAsLookup(resolvedTypeRef, anonymousFunction.source, context.file.source)
        }

        val result = transformElement(anonymousFunction, null)

        val returnExpressionsOfAnonymousFunction: Collection<FirStatement> =
            dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(anonymousFunction)
        for (expression in returnExpressionsOfAnonymousFunction) {
            expression.transform<FirElement, ExpectedArgumentType?>(this, finalType?.toExpectedType())
        }

        val resultFunction = result
        if (resultFunction.returnTypeRef.coneTypeSafe<ConeIntegerLiteralType>() != null) {
            val lastExpressionType =
                (returnExpressionsOfAnonymousFunction.lastOrNull() as? FirExpression)
                    ?.typeRef?.coneTypeSafe<ConeKotlinType>()

            val newReturnTypeRef = resultFunction.returnTypeRef.withReplacedConeType(lastExpressionType)
            resultFunction.replaceReturnTypeRef(newReturnTypeRef)
            val resolvedTypeRef =
                resultFunction.constructFunctionalTypeRef(isSuspend = expectedType?.isSuspendFunctionType(session) == true)
            resultFunction.replaceTypeRef(resolvedTypeRef)
            session.lookupTracker?.let {
                it.recordTypeResolveAsLookup(newReturnTypeRef, anonymousFunction.source, context.file.source)
                it.recordTypeResolveAsLookup(resolvedTypeRef, anonymousFunction.source, context.file.source)
            }
        }

        return result
    }

    private fun FirAnonymousFunction.isSuspendFunctionType() =
        typeRef.coneTypeSafe<ConeKotlinType>()?.isSuspendFunctionType(session) == true

    private fun transformImplicitTypeRefInAnonymousFunction(
        anonymousFunction: FirAnonymousFunction
    ): FirStatement {
        val implicitTypeTransformer = object : FirDefaultTransformer<Any?>() {
            override fun <E : FirElement> transformElement(element: E, data: Any?): E {
                @Suppress("UNCHECKED_CAST")
                return (element.transformChildren(this, data) as E)
            }

            override fun transformImplicitTypeRef(
                implicitTypeRef: FirImplicitTypeRef,
                data: Any?
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
        data: ExpectedArgumentType?
    ): FirStatement {
        val labeledElement = returnExpression.target.labeledElement
        if (labeledElement is FirAnonymousFunction) {
            return returnExpression
        }

        val newData = labeledElement.returnTypeRef.coneTypeSafe<ConeKotlinType>()?.toExpectedType()
        return super.transformReturnExpression(returnExpression, newData)
    }

    override fun transformBlock(block: FirBlock, data: ExpectedArgumentType?): FirStatement {
        val initialType = block.resultType.coneTypeSafe<ConeKotlinType>()
        if (initialType != null) {
            val finalType = finalSubstitutor.substituteOrNull(initialType)
            var resultType = block.resultType.withReplacedConeType(finalType)
            resultType.coneTypeSafe<ConeIntegerLiteralType>()?.let {
                resultType = resultType.resolvedTypeFromPrototype(it.getApproximatedType(data?.getExpectedType(block)))
            }
            block.replaceTypeRef(resultType)
            session.lookupTracker?.recordTypeResolveAsLookup(resultType, block.source, context.file.source)
        }
        transformElement(block, data)
        if (block.resultType is FirErrorTypeRef) {
            block.writeResultType(session)
        }
        return block
    }

    // Transformations for synthetic calls generated by FirSyntheticCallGenerator

    override fun transformWhenExpression(
        whenExpression: FirWhenExpression,
        data: ExpectedArgumentType?
    ): FirStatement {
        return transformSyntheticCall(whenExpression, data)
    }

    override fun transformTryExpression(
        tryExpression: FirTryExpression,
        data: ExpectedArgumentType?
    ): FirStatement {
        return transformSyntheticCall(tryExpression, data)
    }

    override fun transformCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: ExpectedArgumentType?
    ): FirStatement {
        return transformSyntheticCall(checkNotNullCall, data)
    }

    override fun transformElvisExpression(
        elvisExpression: FirElvisExpression,
        data: ExpectedArgumentType?
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
        syntheticCall.replaceTypeRefWithSubstituted(calleeReference, typeRef)
        transformSyntheticCallChildren(syntheticCall, data)

        return (syntheticCall.transformCalleeReference(
            StoreCalleeReference,
            calleeReference.toResolvedReference(),
        ) as D)
    }

    private inline fun <reified D> transformSyntheticCallChildren(
        syntheticCall: D,
        data: ExpectedArgumentType?
    ) where D : FirResolvable, D : FirExpression {
        val newData = data?.getExpectedType(syntheticCall)?.toExpectedType() ?: syntheticCall.typeRef.coneType.toExpectedType()

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

    override fun <T> transformConstExpression(
        constExpression: FirConstExpression<T>,
        data: ExpectedArgumentType?,
    ): FirStatement {
        if (data == ExpectedArgumentType.NoApproximation) return constExpression
        val expectedType = data?.getExpectedType(constExpression)
        if (expectedType is ConeIntegerConstantOperatorType) {
            return constExpression
        }
        return constExpression.transformSingle(integerOperatorApproximator, expectedType)
    }

    override fun transformIntegerLiteralOperatorCall(
        integerLiteralOperatorCall: FirIntegerLiteralOperatorCall,
        data: ExpectedArgumentType?
    ): FirStatement {
        if (data == ExpectedArgumentType.NoApproximation) return integerLiteralOperatorCall
        val expectedType = data?.getExpectedType(integerLiteralOperatorCall)
        if (expectedType is ConeIntegerConstantOperatorType) {
            return integerLiteralOperatorCall
        }
        return integerLiteralOperatorCall.transformSingle(integerOperatorApproximator, expectedType)
    }

    override fun transformArrayOfCall(arrayOfCall: FirArrayOfCall, data: ExpectedArgumentType?): FirStatement {
        if (arrayOfCall.typeRef !is FirImplicitTypeRef) return arrayOfCall
        val expectedArrayType = data?.getExpectedType(arrayOfCall)
        val expectedArrayElementType = expectedArrayType?.arrayElementType()
        arrayOfCall.transformChildren(this, expectedArrayElementType?.toExpectedType())
        val arrayElementType =
            session.typeContext.commonSuperTypeOrNull(arrayOfCall.arguments.map { it.typeRef.coneType })?.let {
                typeApproximator.approximateToSuperType(it, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference)
                    ?: it
            } ?: expectedArrayElementType ?: session.builtinTypes.nullableAnyType.type
        arrayOfCall.resultType = arrayOfCall.typeRef.resolvedTypeFromPrototype(
            arrayElementType.createArrayType(createPrimitiveArrayTypeIfPossible = expectedArrayType?.isPrimitiveArray == true)
        )
        return arrayOfCall
    }

    override fun transformVarargArgumentsExpression(
        varargArgumentsExpression: FirVarargArgumentsExpression,
        data: ExpectedArgumentType?
    ): FirStatement {
        val expectedType = data?.getExpectedType(varargArgumentsExpression)?.let { ExpectedArgumentType.ExpectedType(it) }
        varargArgumentsExpression.transformChildren(this, expectedType)
        return varargArgumentsExpression
    }

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
            else -> null
        }

        return if (errorDiagnostic != null) {
            buildErrorNamedReference {
                source = this@toResolvedReference.source
                diagnostic = errorDiagnostic
                candidateSymbol = this@toResolvedReference.candidateSymbol
            }
        } else {
            buildResolvedNamedReference {
                source = this@toResolvedReference.source
                name = this@toResolvedReference.name
                resolvedSymbol = this@toResolvedReference.candidateSymbol
            }
        }
    }
}

sealed class ExpectedArgumentType {
    class ArgumentsMap(
        val map: Map<FirElement, ConeKotlinType>,
        val lambdasReturnTypes: Map<FirAnonymousFunction, ConeKotlinType>
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

internal class FirDeclarationCompletionResultsWriter(
    private val finalSubstitutor: ConeSubstitutor,
    private val typeApproximator: ConeTypeApproximator,
    private val typeContext: ConeInferenceContext
) : FirDefaultTransformer<FirDeclarationCompletionResultsWriter.ApproximationData>() {
    override fun <E : FirElement> transformElement(element: E, data: ApproximationData): E {
        return element
    }

    override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: ApproximationData): FirStatement {
        return super.transformAnonymousObject(anonymousObject, ApproximationData.NoApproximation)
    }

    override fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: ApproximationData): FirStatement {
        val newData = if (simpleFunction.isLocal || data == ApproximationData.NoApproximation) ApproximationData.NoApproximation
        else ApproximationData.ApproximateByStatus(simpleFunction.visibility, simpleFunction.isInline)
        simpleFunction.transformReturnTypeRef(this, newData)
        simpleFunction.transformValueParameters(this, ApproximationData.NoApproximation)
        simpleFunction.transformReceiverTypeRef(this, newData)
        return simpleFunction
    }

    override fun transformProperty(property: FirProperty, data: ApproximationData): FirStatement {
        val newData = if (property.isLocal || data == ApproximationData.NoApproximation) ApproximationData.NoApproximation
        else ApproximationData.ApproximateByStatus(property.visibility, false)
        property.transformGetter(this, newData)
        property.transformSetter(this, newData)
        property.transformReturnTypeRef(this, newData)
        property.transformReceiverTypeRef(this, newData)
        return property
    }

    override fun transformPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: ApproximationData): FirStatement {
        propertyAccessor.transformReturnTypeRef(this, data)
        propertyAccessor.transformValueParameters(this, ApproximationData.NoApproximation)
        return propertyAccessor
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: ApproximationData): FirStatement {
        valueParameter.transformReturnTypeRef(this, ApproximationData.NoApproximation)
        return valueParameter
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: ApproximationData): FirTypeRef {
        val result = finalSubstitutor.substituteOrNull(typeRef.coneType)?.let {
            typeRef.resolvedTypeFromPrototype(it)
        } ?: typeRef
        if (data is ApproximationData.ApproximateByStatus) {
            return result.approximatedIfNeededOrSelf(typeApproximator, data.visibility, data.isInline)
        }
        return result
    }

    sealed class ApproximationData {
        class ApproximateByStatus(val visibility: Visibility?, val isInline: Boolean) : ApproximationData()
        object NoApproximation : ApproximationData()
        object Default : ApproximationData()
    }
}
