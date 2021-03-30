/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedCallableReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.dfa.FirDataFlowAnalyzer
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeTypeParameterInQualifiedAccess
import org.jetbrains.kotlin.fir.resolve.inference.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirArrayOfCallTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.remapArgumentsWithVararg
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.writeResultType
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperatorCall
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildStarProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirCallCompletionResultsWriterTransformer(
    override val session: FirSession,
    private val finalSubstitutor: ConeSubstitutor,
    private val typeCalculator: ReturnTypeCalculator,
    private val typeApproximator: AbstractTypeApproximator,
    private val dataFlowAnalyzer: FirDataFlowAnalyzer<*>,
    private val mode: Mode = Mode.Normal
) : FirAbstractTreeTransformer<ExpectedArgumentType?>(phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {

    private val declarationWriter by lazy { FirDeclarationCompletionResultsWriter(finalSubstitutor) }

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
        qualifiedAccessExpression: T, calleeReference: FirNamedReferenceWithCandidate
    ): T {
        val subCandidate = calleeReference.candidate
        val declaration: FirDeclaration = subCandidate.symbol.fir as FirDeclaration
        val typeArguments = computeTypeArguments(qualifiedAccessExpression, subCandidate)
        val typeRef = if (declaration is FirTypedDeclaration) {
            val calculated = typeCalculator.tryCalculateReturnType(declaration)
            if (calculated !is FirErrorTypeRef) {
                buildResolvedTypeRef {
                    source = calculated.source?.fakeElement(FirFakeSourceElementKind.ImplicitTypeRef)
                    annotations += calculated.annotations
                    type = calculated.type
                }
            } else {
                buildErrorTypeRef {
                    source = calculated.source?.fakeElement(FirFakeSourceElementKind.ImplicitTypeRef)
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
                source = calleeReference.source?.fakeElement(FirFakeSourceElementKind.ImplicitTypeRef)
                diagnostic =
                    when (declaration) {
                        is FirTypeParameter -> ConeTypeParameterInQualifiedAccess(declaration.symbol)
                        is FirResolvedReifiedParameterReference -> ConeTypeParameterInQualifiedAccess(declaration.symbol)
                        else -> ConeSimpleDiagnostic("Callee reference to candidate without return type: ${declaration.render()}")
                    }
            }
        }

        @Suppress("UNCHECKED_CAST")
        val result = qualifiedAccessExpression
            .transformCalleeReference(
                StoreCalleeReference,
                calleeReference.toResolvedReference(),
            )
            .transformDispatchReceiver(StoreReceiver, subCandidate.dispatchReceiverExpression())
            .transformExtensionReceiver(StoreReceiver, subCandidate.extensionReceiverExpression()) as T
        result.replaceTypeRef(typeRef)
        if (declaration !is FirErrorFunction) {
            result.replaceTypeArguments(typeArguments)
        }
        session.lookupTracker?.recordTypeResolveAsLookup(typeRef, qualifiedAccessExpression.source, null)
        return result
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: ExpectedArgumentType?,
    ): CompositeTransformResult<FirStatement> {
        val calleeReference = qualifiedAccessExpression.calleeReference as? FirNamedReferenceWithCandidate
            ?: return run {
                if (mode == Mode.DelegatedPropertyCompletion) {
                    val typeUpdater = TypeUpdaterForDelegateArguments()
                    qualifiedAccessExpression.transformSingle(typeUpdater, null)
                }
                qualifiedAccessExpression.compose()
            }
        val result = prepareQualifiedTransform(qualifiedAccessExpression, calleeReference)
        val typeRef = result.typeRef as FirResolvedTypeRef
        val subCandidate = calleeReference.candidate

        val resultType = typeRef.substituteTypeRef(subCandidate)
        resultType.ensureResolvedTypeDeclaration(session)
        result.replaceTypeRef(resultType)
        session.lookupTracker?.recordTypeResolveAsLookup(resultType, qualifiedAccessExpression.source, null)

        if (mode == Mode.DelegatedPropertyCompletion) {
            subCandidate.symbol.fir.transformSingle(declarationWriter, null)
            val typeUpdater = TypeUpdaterForDelegateArguments()
            result.transformExplicitReceiver(typeUpdater, null)
        }

        return result.compose()
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: ExpectedArgumentType?): CompositeTransformResult<FirStatement> {
        val calleeReference = functionCall.calleeReference as? FirNamedReferenceWithCandidate
            ?: return functionCall.compose()
        var result = prepareQualifiedTransform(functionCall, calleeReference)
        val typeRef = result.typeRef as FirResolvedTypeRef
        val subCandidate = calleeReference.candidate
        val resultType: FirTypeRef
        result = when (result) {
            is FirIntegerOperatorCall -> {
                val expectedType = data?.getExpectedType(functionCall)
                resultType =
                    typeRef.resolvedTypeFromPrototype(typeRef.coneTypeUnsafe<ConeIntegerLiteralType>().getApproximatedType(expectedType))
                result.argumentList.transformArguments(this, expectedType?.toExpectedType())
                result
            }
            else -> {
                resultType = typeRef.substituteTypeRef(subCandidate)
                val expectedArgumentsTypeMapping = runIf(!calleeReference.isError) { subCandidate.createArgumentsMapping() }
                result.argumentList.transformArguments(this, expectedArgumentsTypeMapping)
                if (calleeReference.isError) {
                    subCandidate.argumentMapping?.let {
                        result.replaceArgumentList(buildPartiallyResolvedArgumentList(result.argumentList, it))
                    }
                } else {
                    subCandidate.handleVarargs()
                    subCandidate.argumentMapping?.let {
                        result.replaceArgumentList(buildResolvedArgumentList(it))
                    }
                }
                result
            }
        }

        result.replaceTypeRef(resultType)
        session.lookupTracker?.recordTypeResolveAsLookup(resultType, functionCall.source, null)

        if (mode == Mode.DelegatedPropertyCompletion) {
            subCandidate.symbol.fir.transformSingle(declarationWriter, null)
            val typeUpdater = TypeUpdaterForDelegateArguments()
            result.argumentList.transformArguments(typeUpdater, null)
            result.transformExplicitReceiver(typeUpdater, null)
        }

        if (enableArrayOfCallTransformation) {
            arrayOfCallTransformer.toArrayOfCall(result)?.let {
                return it.compose()
            }
        }

        return result.compose()
    }

    override fun transformAnnotationCall(
        annotationCall: FirAnnotationCall,
        data: ExpectedArgumentType?
    ): CompositeTransformResult<FirStatement> {
        val calleeReference = annotationCall.calleeReference as? FirNamedReferenceWithCandidate
            ?: return annotationCall.compose()
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
                annotationCall.replaceArgumentList(buildPartiallyResolvedArgumentList(annotationCall.argumentList, it))
            }
        } else {
            subCandidate.handleVarargs()
            subCandidate.argumentMapping?.let {
                annotationCall.replaceArgumentList(buildResolvedArgumentList(it))
            }
        }
        return annotationCall.compose()
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
        session.lookupTracker?.recordTypeResolveAsLookup(resultTypeRef, source, null)
        return this
    }

    private fun FirResolvedTypeRef.substituteTypeRef(
        candidate: Candidate,
    ): FirResolvedTypeRef {
        val initialType = candidate.substitutor.substituteOrSelf(type)
        val substitutedType = finalSubstitutor.substituteOrNull(initialType)
        val finalType = typeApproximator.approximateToSuperType(
            type = substitutedType ?: initialType, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference,
        ) as ConeKotlinType? ?: substitutedType

        return withReplacedConeType(finalType)
    }

    override fun transformSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: ExpectedArgumentType?
    ): CompositeTransformResult<FirStatement> {
        safeCallExpression.transformRegularQualifiedAccess(
            this,
            data?.getExpectedType(
                safeCallExpression
            )?.toExpectedType()
        )

        safeCallExpression.propagateTypeFromQualifiedAccessAfterNullCheck(safeCallExpression.receiver, session)

        return safeCallExpression.compose()
    }

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: ExpectedArgumentType?,
    ): CompositeTransformResult<FirStatement> {
        val calleeReference =
            callableReferenceAccess.calleeReference as? FirNamedReferenceWithCandidate ?: return callableReferenceAccess.compose()
        val subCandidate = calleeReference.candidate
        val typeArguments = computeTypeArguments(callableReferenceAccess, subCandidate)

        val typeRef = callableReferenceAccess.typeRef as FirResolvedTypeRef

        val initialType = calleeReference.candidate.substitutor.substituteOrSelf(typeRef.type)
        val finalType = finalSubstitutor.substituteOrSelf(initialType)

        val resultType = typeRef.withReplacedConeType(finalType)
        callableReferenceAccess.replaceTypeRef(resultType)
        callableReferenceAccess.replaceTypeArguments(typeArguments)
        session.lookupTracker?.recordTypeResolveAsLookup(resultType, typeRef.source ?: callableReferenceAccess.source, null)

        return callableReferenceAccess.transformCalleeReference(
            StoreCalleeReference,
            buildResolvedCallableReference {
                source = calleeReference.source
                name = calleeReference.name
                resolvedSymbol = calleeReference.candidateSymbol
                inferredTypeArguments.addAll(computeTypeArgumentTypes(calleeReference.candidate))
                mappedArguments = subCandidate.callableReferenceAdaptation?.mappedArguments ?: emptyMap()
            },
        ).transformDispatchReceiver(StoreReceiver, subCandidate.dispatchReceiverExpression())
            .transformExtensionReceiver(StoreReceiver, subCandidate.extensionReceiverExpression())
            .compose()
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: ExpectedArgumentType?,
    ): CompositeTransformResult<FirStatement> {
        val calleeReference = variableAssignment.calleeReference as? FirNamedReferenceWithCandidate
            ?: return variableAssignment.compose()
        val typeArguments = computeTypeArguments(variableAssignment, calleeReference.candidate)
        return variableAssignment.transformCalleeReference(
            StoreCalleeReference,
            calleeReference.toResolvedReference(),
        ).apply {
            replaceTypeArguments(typeArguments)
        }.compose()
    }

    private inner class TypeUpdaterForDelegateArguments : FirTransformer<Nothing?>() {
        override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
            return element.compose()
        }

        override fun transformQualifiedAccessExpression(
            qualifiedAccessExpression: FirQualifiedAccessExpression,
            data: Nothing?
        ): CompositeTransformResult<FirStatement> {
            val originalType = qualifiedAccessExpression.typeRef.coneType
            val substitutedReceiverType = finalSubstitutor.substituteOrNull(originalType) ?: return qualifiedAccessExpression.compose()
            val resolvedTypeRef = qualifiedAccessExpression.typeRef.resolvedTypeFromPrototype(substitutedReceiverType)
            qualifiedAccessExpression.replaceTypeRef(resolvedTypeRef)
            session.lookupTracker?.recordTypeResolveAsLookup(resolvedTypeRef, qualifiedAccessExpression.source, null)
            return qualifiedAccessExpression.compose()
        }
    }

    private fun FirTypeRef.substitute(candidate: Candidate): ConeKotlinType =
        coneType.let { candidate.substitutor.substituteOrSelf(it) }
            .let { finalSubstitutor.substituteOrSelf(it) }

    private fun Candidate.createArgumentsMapping(): ExpectedArgumentType? {
        val lambdasReturnType = postponedAtoms.filterIsInstance<ResolvedLambdaAtom>().associate {
            Pair(it.atom, finalSubstitutor.substituteOrSelf(substitutor.substituteOrSelf(it.returnType)))
        }

        val arguments = argumentMapping?.map { (argument, valueParameter) ->
            val expectedType = if (valueParameter.isVararg) {
                valueParameter.returnTypeRef.substitute(this).varargElementType()
            } else {
                valueParameter.returnTypeRef.substitute(this)
            }
            argument.unwrapArgument() to expectedType
        }?.toMap()


        if (lambdasReturnType.isEmpty() && arguments.isNullOrEmpty()) return null
        return ExpectedArgumentType.ArgumentsMap(arguments ?: emptyMap(), lambdasReturnType)
    }

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: ExpectedArgumentType?,
    ): CompositeTransformResult<FirStatement> {
        val calleeReference =
            delegatedConstructorCall.calleeReference as? FirNamedReferenceWithCandidate ?: return delegatedConstructorCall.compose()
        val subCandidate = calleeReference.candidate

        val argumentsMapping = runIf(!calleeReference.isError) { calleeReference.candidate.createArgumentsMapping() }
        delegatedConstructorCall.argumentList.transformArguments(this, argumentsMapping)
        if (calleeReference.isError) {
            subCandidate.argumentMapping?.let {
                delegatedConstructorCall.replaceArgumentList(buildPartiallyResolvedArgumentList(delegatedConstructorCall.argumentList, it))
            }
        } else {
            subCandidate.handleVarargs()
            subCandidate.argumentMapping?.let {
                delegatedConstructorCall.replaceArgumentList(buildResolvedArgumentList(it))
            }
        }
        return delegatedConstructorCall.transformCalleeReference(
            StoreCalleeReference,
            calleeReference.toResolvedReference(),
        ).compose()
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
                            this.typeRef = if (typeRef.type is ConeClassErrorType) typeRef else typeRef.withReplacedConeType(type)
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
        val declaration = candidate.symbol.fir as? FirCallableMemberDeclaration<*> ?: return emptyList()

        return declaration.typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }
            .map { candidate.substitutor.substituteOrSelf(it) }
            .map {
                finalSubstitutor.substituteOrSelf(it).let { substitutedType ->
                    typeApproximator.approximateToSuperType(
                        substitutedType, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference,
                    ) as ConeKotlinType? ?: substitutedType
                }
            }
    }

    override fun transformAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        data: ExpectedArgumentType?,
    ): CompositeTransformResult<FirStatement> {
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
                    val sam =
                        (session.firProvider.getFirClassifierByFqName(expectedArgumentType.lookupTag.classId) as? FirClass)
                            ?.takeIf { it.classKind == ClassKind.INTERFACE }
                            ?.declarations?.singleOrNull() as? FirSimpleFunction
                    sam?.let {
                        createFunctionalType(
                            sam.valueParameters.map { it.returnTypeRef.coneType }, null, sam.returnTypeRef.coneType, sam.isSuspend
                        )
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
        val finalType =
            expectedType?.returnType(session) as? ConeClassLikeType
                ?: (data as? ExpectedArgumentType.ArgumentsMap)?.lambdasReturnTypes?.get(anonymousFunction)
                ?: initialType?.let(finalSubstitutor::substituteOrSelf)

        if (finalType != null) {
            val resultType = anonymousFunction.returnTypeRef.withReplacedConeType(finalType)
            anonymousFunction.transformReturnTypeRef(StoreType, resultType)
            needUpdateLambdaType = true
        }

        if (needUpdateLambdaType) {
            val resolvedTypeRef = anonymousFunction.constructFunctionalTypeRef(isSuspend = expectedType?.isSuspendFunctionType(session) == true)
            anonymousFunction.replaceTypeRef(resolvedTypeRef)
            session.lookupTracker?.recordTypeResolveAsLookup(resolvedTypeRef, anonymousFunction.source, null)
        }

        val result = transformElement(anonymousFunction, null)

        val returnExpressionsOfAnonymousFunction: Collection<FirStatement> =
            dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(anonymousFunction)
        for (expression in returnExpressionsOfAnonymousFunction) {
            expression.transform<FirElement, ExpectedArgumentType?>(this, finalType?.toExpectedType())
        }

        val resultFunction = result.single
        if (resultFunction.returnTypeRef.coneTypeSafe<ConeIntegerLiteralType>() != null) {
            val lastExpressionType =
                (returnExpressionsOfAnonymousFunction.lastOrNull() as? FirExpression)
                    ?.typeRef?.coneTypeSafe<ConeKotlinType>()

            val newReturnTypeRef = resultFunction.returnTypeRef.withReplacedConeType(lastExpressionType)
            resultFunction.replaceReturnTypeRef(newReturnTypeRef)
            val resolvedTypeRef = resultFunction.constructFunctionalTypeRef(isSuspend = expectedType?.isSuspendFunctionType(session) == true)
            resultFunction.replaceTypeRef(resolvedTypeRef)
            session.lookupTracker?.let {
                it.recordTypeResolveAsLookup(newReturnTypeRef, anonymousFunction.source, null)
                it.recordTypeResolveAsLookup(resolvedTypeRef, anonymousFunction.source, null)
            }
        }

        return result
    }

    private fun transformImplicitTypeRefInAnonymousFunction(
        anonymousFunction: FirAnonymousFunction
    ): CompositeTransformResult<FirStatement> {
        val implicitTypeTransformer = object : FirDefaultTransformer<Nothing?>() {
            override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
                @Suppress("UNCHECKED_CAST")
                return (element.transformChildren(this, data) as E).compose()
            }

            override fun transformImplicitTypeRef(
                implicitTypeRef: FirImplicitTypeRef,
                data: Nothing?
            ): CompositeTransformResult<FirTypeRef> =
                buildErrorTypeRef {
                    source = implicitTypeRef.source
                    // NB: this error message assumes that it is used only if CFG for the anonymous function is not available
                    diagnostic = ConeSimpleDiagnostic("Cannot infer type w/o CFG", DiagnosticKind.InferenceError)
                }.compose()

        }
        // NB: if we transform simply all children, there would be too many type error reports.
        anonymousFunction.transformReturnTypeRef(implicitTypeTransformer, null)
        anonymousFunction.transformValueParameters(implicitTypeTransformer, null)
        anonymousFunction.transformBody(implicitTypeTransformer, null)
        return anonymousFunction.compose()
    }

    override fun transformReturnExpression(
        returnExpression: FirReturnExpression,
        data: ExpectedArgumentType?
    ): CompositeTransformResult<FirStatement> {
        val labeledElement = returnExpression.target.labeledElement
        if (labeledElement is FirAnonymousFunction) {
            return returnExpression.compose()
        }

        return super.transformReturnExpression(returnExpression, data)
    }

    override fun transformBlock(block: FirBlock, data: ExpectedArgumentType?): CompositeTransformResult<FirStatement> {
        val initialType = block.resultType.coneTypeSafe<ConeKotlinType>()
        if (initialType != null) {
            val finalType = finalSubstitutor.substituteOrNull(initialType)
            var resultType = block.resultType.withReplacedConeType(finalType)
            resultType.coneTypeSafe<ConeIntegerLiteralType>()?.let {
                resultType = resultType.resolvedTypeFromPrototype(it.getApproximatedType(data?.getExpectedType(block)))
            }
            block.replaceTypeRef(resultType)
            session.lookupTracker?.recordTypeResolveAsLookup(resultType, block.source, null)
        }
        transformElement(block, data)
        if (block.resultType is FirErrorTypeRef) {
            block.writeResultType(session)
        }
        return block.compose()
    }

    // Transformations for synthetic calls generated by FirSyntheticCallGenerator

    override fun transformWhenExpression(
        whenExpression: FirWhenExpression,
        data: ExpectedArgumentType?
    ): CompositeTransformResult<FirStatement> {
        return transformSyntheticCall(whenExpression, data)
    }

    override fun transformTryExpression(
        tryExpression: FirTryExpression,
        data: ExpectedArgumentType?
    ): CompositeTransformResult<FirStatement> {
        return transformSyntheticCall(tryExpression, data)
    }

    override fun transformCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: ExpectedArgumentType?
    ): CompositeTransformResult<FirStatement> {
        return transformSyntheticCall(checkNotNullCall, data)
    }

    override fun transformElvisExpression(
        elvisExpression: FirElvisExpression,
        data: ExpectedArgumentType?
    ): CompositeTransformResult<FirStatement> {
        return transformSyntheticCall(elvisExpression, data)
    }

    private inline fun <reified D> transformSyntheticCall(
        syntheticCall: D,
        data: ExpectedArgumentType?,
    ): CompositeTransformResult<FirStatement> where D : FirResolvable, D : FirExpression {
        val calleeReference = syntheticCall.calleeReference as? FirNamedReferenceWithCandidate
        val declaration = calleeReference?.candidate?.symbol?.fir as? FirSimpleFunction

        if (calleeReference == null || declaration == null) {
            transformSyntheticCallChildren(syntheticCall, data)
            return syntheticCall.compose()
        }

        val typeRef = typeCalculator.tryCalculateReturnType(declaration)
        syntheticCall.replaceTypeRefWithSubstituted(calleeReference, typeRef)
        transformSyntheticCallChildren(syntheticCall, data)

        return (syntheticCall.transformCalleeReference(
            StoreCalleeReference,
            calleeReference.toResolvedReference(),
        ) as D).compose()
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
    ): CompositeTransformResult<FirStatement> {
        if (data == ExpectedArgumentType.NoApproximation) return constExpression.compose()
        return constExpression.approximateIfIsIntegerConst(data?.getExpectedType(constExpression)).compose()
    }

    override fun transformArrayOfCall(arrayOfCall: FirArrayOfCall, data: ExpectedArgumentType?): CompositeTransformResult<FirStatement> {
        if (arrayOfCall.typeRef !is FirImplicitTypeRef) return arrayOfCall.compose()
        val expectedArrayType = data?.getExpectedType(arrayOfCall)
        val expectedArrayElementType = expectedArrayType?.arrayElementType()
        arrayOfCall.transformChildren(this, expectedArrayElementType?.toExpectedType())
        val arrayElementType = session.inferenceComponents.ctx.commonSuperTypeOrNull(arrayOfCall.arguments.map { it.typeRef.coneType })
            ?: session.builtinTypes.nullableAnyType.type
        arrayOfCall.resultType = arrayOfCall.typeRef.resolvedTypeFromPrototype(arrayElementType.createArrayType())
        return arrayOfCall.compose()
    }

    private fun FirNamedReferenceWithCandidate.toResolvedReference() = if (this is FirErrorReferenceWithCandidate) {
        buildErrorNamedReference {
            source = this@toResolvedReference.source
            diagnostic = this@toResolvedReference.diagnostic
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

sealed class ExpectedArgumentType {
    class ArgumentsMap(
        val map: Map<FirExpression, ConeKotlinType>,
        val lambdasReturnTypes: Map<FirAnonymousFunction, ConeKotlinType>
    ) : ExpectedArgumentType()

    class ExpectedType(val type: ConeKotlinType) : ExpectedArgumentType()
    object NoApproximation : ExpectedArgumentType()
}

private fun ExpectedArgumentType.getExpectedType(argument: FirExpression): ConeKotlinType? = when (this) {
    is ExpectedArgumentType.ArgumentsMap -> map[argument]
    is ExpectedArgumentType.ExpectedType -> type
    ExpectedArgumentType.NoApproximation -> null
}

fun ConeKotlinType.toExpectedType(): ExpectedArgumentType = ExpectedArgumentType.ExpectedType(this)

private fun FirExpression.unwrapArgument(): FirExpression = when (this) {
    is FirWrappedArgumentExpression -> expression
    else -> this
}

class FirDeclarationCompletionResultsWriter(private val finalSubstitutor: ConeSubstitutor) : FirDefaultTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        simpleFunction.transformReturnTypeRef(this, data)
        simpleFunction.transformValueParameters(this, data)
        simpleFunction.transformReceiverTypeRef(this, data)
        return simpleFunction.compose()
    }

    override fun transformProperty(property: FirProperty, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        property.transformGetter(this, data)
        property.transformSetter(this, data)
        property.transformReturnTypeRef(this, data)
        property.transformReceiverTypeRef(this, data)
        return property.compose()
    }

    override fun transformPropertyAccessor(
        propertyAccessor: FirPropertyAccessor,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        propertyAccessor.transformReturnTypeRef(this, data)
        propertyAccessor.transformValueParameters(this, data)
        return propertyAccessor.compose()
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        valueParameter.transformReturnTypeRef(this, data)
        return valueParameter.compose()
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> {
        return finalSubstitutor.substituteOrNull(typeRef.coneType)?.let {
            typeRef.resolvedTypeFromPrototype(it)
        }?.compose() ?: typeRef.compose()
    }
}
