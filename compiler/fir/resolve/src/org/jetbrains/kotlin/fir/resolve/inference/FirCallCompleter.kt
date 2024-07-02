/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildContextReceiver
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferReceiverParameterType
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferValueParameterType
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.stages.TypeArgumentMapping
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeArgumentConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.jetbrains.kotlin.fir.resolve.initialTypeOfCandidate
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.FirCallCompletionResultsWriterTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.transformers.replaceLambdaArgumentInvocationKinds
import org.jetbrains.kotlin.fir.resolve.typeFromCallee
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SyntheticCallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.resolve.calls.inference.addEqualityConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.safeSubstitute
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirCallCompleter(
    private val transformer: FirAbstractBodyResolveTransformerDispatcher,
    private val components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
) {
    private val session = components.session
    private val inferenceSession
        get() = transformer.context.inferenceSession

    val completer: ConstraintSystemCompleter = ConstraintSystemCompleter(components)

    fun <T> completeCall(
        call: T,
        resolutionMode: ResolutionMode,
        // Only expected to be true for resolving different versions of augmented assignments
        skipEvenPartialCompletion: Boolean = false,
    ): T where T : FirResolvable, T : FirExpression {
        val typeRef = components.typeFromCallee(call)

        val reference = call.calleeReference as? FirNamedReferenceWithCandidate ?: return call

        val candidate = reference.candidate
        val initialType = typeRef.initialTypeOfCandidate(candidate)

        call.resultType = initialType
        session.lookupTracker?.recordTypeResolveAsLookup(initialType, call.source, components.context.file.source)

        addConstraintFromExpectedType(
            candidate,
            initialType,
            resolutionMode,
        )

        if (skipEvenPartialCompletion) return call

        val completionMode = candidate.computeCompletionMode(
            session.inferenceComponents, resolutionMode, initialType
        ).let {
            when {
                it == ConstraintSystemCompletionMode.FULL ->
                    inferenceSession.customCompletionModeInsteadOfFull(call) ?: ConstraintSystemCompletionMode.FULL
                else -> it
            }
        }

        val analyzer = createPostponedArgumentsAnalyzer(transformer.resolutionContext)
        if (call is FirFunctionCall) {
            call.replaceLambdaArgumentInvocationKinds(session)
        }

        return when (completionMode) {
            ConstraintSystemCompletionMode.FULL -> {
                runCompletionForCall(candidate, completionMode, call, initialType, analyzer)

                val readOnlyConstraintStorage = candidate.system.asReadOnlyStorage()
                if (inferenceSession !is FirPCLAInferenceSession) {
                    // With FirPCLAInferenceSession we either have here a situation when the candidate system uses
                    // an outer system, and completionMode is PCLA_POSTPONED_CALL (see customCompletionModeInsteadOfFull);
                    // or, the candidate system does not use an outer system; then the PCLA common system is somewhere on top,
                    // and after fixing type variables of this candidate we still may have some unfixed variables from the common system
                    // TODO: KT-69040. We think some better decision is possible here, e.g. in a situation when the candidate system
                    // does not use an outer system either the completionMode should not be FULL,
                    // or it should not be added as a "base" system.
                    checkStorageConstraintsAfterFullCompletion(readOnlyConstraintStorage)
                }

                val finalSubstitutor = readOnlyConstraintStorage
                    .buildAbstractResultingSubstitutor(session.typeContext) as ConeSubstitutor
                call.transformSingle(
                    FirCallCompletionResultsWriterTransformer(
                        session, components.scopeSession, finalSubstitutor,
                        components.returnTypeCalculator,
                        session.typeApproximator,
                        components.dataFlowAnalyzer,
                        components.integerLiteralAndOperatorApproximationTransformer,
                        components.samResolver,
                        components.context,
                    ),
                    null
                )
            }

            ConstraintSystemCompletionMode.PARTIAL, ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL -> {
                runCompletionForCall(candidate, completionMode, call, initialType, analyzer)

                inferenceSession.processPartiallyResolvedCall(call, resolutionMode, completionMode)

                call
            }

            ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA -> throw IllegalStateException()
        }
    }

    private fun checkStorageConstraintsAfterFullCompletion(storage: ConstraintStorage) {
        // Fast path for sake of optimization
        if (storage.notFixedTypeVariables.isEmpty()) return

        val notFixedTypeVariablesBasedOnTypeParameters = storage.notFixedTypeVariables.filter {
            it.value.typeVariable is ConeTypeParameterBasedTypeVariable
        }

        // TODO: Turn it into `require(storage.notFixedTypeVariables.isEmpty())` (KT-66759)
        require(notFixedTypeVariablesBasedOnTypeParameters.isEmpty()) {
            "All variables should be fixed to something, " +
                    "but {${notFixedTypeVariablesBasedOnTypeParameters.keys.joinToString(", ")}} are found"
        }
    }

    private fun addConstraintFromExpectedType(
        candidate: Candidate,
        initialType: ConeKotlinType,
        resolutionMode: ResolutionMode,
    ) {
        if (resolutionMode !is ResolutionMode.WithExpectedType) return
        val expectedType = resolutionMode.expectedTypeRef.type.fullyExpandedType(session)

        val system = candidate.system
        when {
            // Only add equality constraint in independent contexts (resolutionMode.forceFullCompletion) for K1 compatibility.
            // Otherwise,
            // we miss some constraints from incorporation which leads to NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER in cases like
            // compiler/testData/diagnostics/tests/inference/nestedIfWithExpectedType.kt.
            resolutionMode.forceFullCompletion && candidate.isSyntheticFunctionCallThatShouldUseEqualityConstraint(expectedType) ->
                system.addEqualityConstraintIfCompatible(initialType, expectedType, ConeExpectedTypeConstraintPosition)

            // If type mismatch is assumed to be reported in the checker, we should not add a subtyping constraint that leads to error.
            // Because it might make resulting type correct while, it's hopefully would be more clear if we let the call be inferred without
            // the expected type, and then would report diagnostic in the checker.
            // It's assumed to be safe & sound, because if constraint system has contradictions when expected type is added,
            // the resulting expression type cannot be inferred to something that is a subtype of `expectedType`,
            // thus the diagnostic should be reported.
            !resolutionMode.shouldBeStrictlyEnforced || resolutionMode.expectedTypeMismatchIsReportedInChecker -> {
                system.addSubtypeConstraintIfCompatible(initialType, expectedType, ConeExpectedTypeConstraintPosition)
            }
            resolutionMode.fromCast -> {
                if (candidate.isFunctionForExpectTypeFromCastFeature()) {
                    system.addSubtypeConstraint(
                        initialType, expectedType,
                        ConeExpectedTypeConstraintPosition,
                    )
                }
            }
            !expectedType.isUnitOrFlexibleUnit || !resolutionMode.mayBeCoercionToUnitApplied -> {
                system.addSubtypeConstraint(initialType, expectedType, ConeExpectedTypeConstraintPosition)
            }
            system.notFixedTypeVariables.isEmpty() -> return
            expectedType.isUnit -> {
                system.addEqualityConstraintIfCompatible(initialType, expectedType, ConeExpectedTypeConstraintPosition)
            }
            else -> {
                system.addSubtypeConstraintIfCompatible(initialType, expectedType, ConeExpectedTypeConstraintPosition)
            }
        }
    }

    /**
     * For synthetic functions (when, try, !!, but **not** elvis), we need to add an equality constraint for the expected type
     * so that some type variables aren't inferred to `Nothing` that appears in one of the branches.
     *
     * @See org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils.createKnownTypeParameterSubstitutorForSpecialCall
     */
    private fun Candidate.isSyntheticFunctionCallThatShouldUseEqualityConstraint(expectedType: ConeKotlinType): Boolean {
        // If we're inside an assignment's RHS, we mustn't add an equality constraint because it might prevent smartcasts.
        // Example: val x: String? = null; x = if (foo) "" else throw Exception()
        if (components.context.isInsideAssignmentRhs) return false

        val symbol = symbol as? FirCallableSymbol ?: return false
        if (symbol.origin != FirDeclarationOrigin.Synthetic.FakeFunction ||
            expectedType.isUnitOrNullableUnit ||
            expectedType.isAnyOrNullableAny ||
            // We don't want to add an equality constraint to a nullable type to a !! call.
            // See compiler/testData/diagnostics/tests/inference/checkNotNullWithNullableExpectedType.kt
            (symbol.callableId == SyntheticCallableId.CHECK_NOT_NULL && expectedType.canBeNull(session))
        ) {
            return false
        }

        // If our expression contains any elvis, even nested, we mustn't add an equality constraint because it might influence the
        // inferred type of the elvis RHS.
        if (system.allTypeVariables.values.any {
                it is ConeTypeParameterBasedTypeVariable && it.typeParameterSymbol.containingDeclarationSymbol.isSyntheticElvisFunction()
            }
        ) {
            return false
        }

        return true
    }

    private fun FirBasedSymbol<*>.isSyntheticElvisFunction(): Boolean {
        return origin == FirDeclarationOrigin.Synthetic.FakeFunction && (this as? FirCallableSymbol)?.callableId == SyntheticCallableId.ELVIS_NOT_NULL
    }

    fun <T> runCompletionForCall(
        candidate: Candidate,
        completionMode: ConstraintSystemCompletionMode,
        call: T,
        initialType: ConeKotlinType,
        analyzer: PostponedArgumentsAnalyzer? = null,
    ) where T : FirExpression, T : FirResolvable {
        @Suppress("NAME_SHADOWING")
        val analyzer = analyzer ?: createPostponedArgumentsAnalyzer(transformer.resolutionContext)
        completer.complete(
            candidate.system.asConstraintSystemCompleterContext(),
            completionMode,
            listOf(ConeAtomWithCandidate(call, candidate)),
            initialType,
            transformer.resolutionContext
        ) { atom, withPCLASession ->
            analyzer.analyze(candidate.system, atom, candidate, withPCLASession)
        }
    }

    fun prepareLambdaAtomForFactoryPattern(
        atom: ConeResolvedLambdaAtom,
        candidate: Candidate,
    ) {
        val returnVariable = ConeTypeVariableForLambdaReturnType(atom.fir, "_R")
        val csBuilder = candidate.system.getBuilder()
        csBuilder.registerVariable(returnVariable)
        val functionalType = csBuilder.buildCurrentSubstitutor()
            .safeSubstitute(csBuilder, atom.expectedType!!) as ConeClassLikeType
        val size = functionalType.typeArguments.size
        val expectedType = ConeClassLikeTypeImpl(
            functionalType.lookupTag,
            Array(size) { index -> if (index != size - 1) functionalType.typeArguments[index] else returnVariable.defaultType },
            isNullable = functionalType.isNullable,
            functionalType.attributes
        )
        csBuilder.addSubtypeConstraint(expectedType, functionalType, ConeArgumentConstraintPosition(atom.fir))
        atom.replaceExpectedType(expectedType, returnVariable.defaultType)
        atom.replaceTypeVariableForLambdaReturnType(returnVariable)
    }

    fun createCompletionResultsWriter(
        substitutor: ConeSubstitutor,
        mode: FirCallCompletionResultsWriterTransformer.Mode = FirCallCompletionResultsWriterTransformer.Mode.Normal,
    ): FirCallCompletionResultsWriterTransformer {
        return FirCallCompletionResultsWriterTransformer(
            session, components.scopeSession, substitutor, components.returnTypeCalculator,
            session.typeApproximator,
            components.dataFlowAnalyzer,
            components.integerLiteralAndOperatorApproximationTransformer,
            components.samResolver,
            components.context,
            mode,
        )
    }

    fun createPostponedArgumentsAnalyzer(context: ResolutionContext): PostponedArgumentsAnalyzer {
        val lambdaAnalyzer = LambdaAnalyzerImpl()
        return PostponedArgumentsAnalyzer(
            context,
            lambdaAnalyzer,
            session.inferenceComponents,
            transformer.components.callResolver
        )
    }

    private inner class LambdaAnalyzerImpl : LambdaAnalyzer {
        override fun analyzeAndGetLambdaReturnArguments(
            lambdaAtom: ConeResolvedLambdaAtom,
            receiverType: ConeKotlinType?,
            contextReceivers: List<ConeKotlinType>,
            parameters: List<ConeKotlinType>,
            expectedReturnType: ConeKotlinType?,
            candidate: Candidate,
            withPCLASession: Boolean,
            forOverloadByLambdaReturnType: Boolean,
        ): ReturnArgumentsAnalysisResult {
            val lambdaArgument: FirAnonymousFunction = lambdaAtom.fir
            val needItParam = lambdaArgument.valueParameters.isEmpty() && parameters.size == 1

            val matchedParameter = candidate.argumentMapping.firstNotNullOfOrNull { (currentAtom, currentValueParameter) ->
                val currentArgument = currentAtom.expression
                val currentLambdaArgument =
                    (currentArgument as? FirAnonymousFunctionExpression)?.anonymousFunction
                if (currentLambdaArgument === lambdaArgument) {
                    currentValueParameter
                } else {
                    null
                }
            }

            lambdaArgument.matchingParameterFunctionType = matchedParameter?.returnTypeRef?.coneType

            val itParam = when {
                needItParam -> {
                    val name = StandardNames.IMPLICIT_LAMBDA_PARAMETER_NAME
                    val itType = parameters.single()
                    buildValueParameter {
                        resolvePhase = FirResolvePhase.BODY_RESOLVE
                        source = lambdaAtom.fir.source?.fakeElement(KtFakeSourceElementKind.ItLambdaParameter)
                        containingFunctionSymbol = lambdaArgument.symbol
                        moduleData = session.moduleData
                        origin = FirDeclarationOrigin.Source
                        this.name = name
                        symbol = FirValueParameterSymbol(name)
                        returnTypeRef =
                            itType.approximateLambdaInputType(symbol, withPCLASession).toFirResolvedTypeRef(
                                lambdaAtom.fir.source?.fakeElement(KtFakeSourceElementKind.ItLambdaParameter)
                            )
                        defaultValue = null
                        isCrossinline = false
                        isNoinline = false
                        isVararg = false
                    }
                }
                else -> null
            }

            val expectedReturnTypeRef = expectedReturnType?.let { lambdaArgument.returnTypeRef.resolvedTypeFromPrototype(it) }

            when {
                receiverType == null -> lambdaArgument.replaceReceiverParameter(null)
                !lambdaAtom.coerceFirstParameterToExtensionReceiver -> {
                    lambdaArgument.receiverParameter?.apply {
                        val type = receiverType.approximateLambdaInputType(valueParameter = null, withPCLASession)
                        val source =
                            source?.fakeElement(KtFakeSourceElementKind.LambdaReceiver)
                                ?: lambdaArgument.source?.fakeElement(KtFakeSourceElementKind.LambdaReceiver)
                        replaceTypeRef(typeRef.resolvedTypeFromPrototype(type, source))
                    }
                }
                else -> lambdaArgument.replaceReceiverParameter(null)
            }

            if (contextReceivers.isNotEmpty()) {
                lambdaArgument.replaceContextReceivers(
                    contextReceivers.map { contextReceiverType ->
                        buildContextReceiver {
                            typeRef = buildResolvedTypeRef {
                                type = contextReceiverType
                            }
                        }
                    }
                )
            }

            val lookupTracker = session.lookupTracker
            val fileSource = components.file.source
            val theParameters = when {
                lambdaAtom.coerceFirstParameterToExtensionReceiver -> when (receiverType) {
                    null -> error("Coercion to extension receiver while no receiver present")
                    else -> listOf(receiverType) + parameters
                }
                else -> parameters
            }
            lambdaArgument.valueParameters.forEachIndexed { index, parameter ->
                if (index >= theParameters.size) {
                    // May happen in erroneous code, see KT-60450
                    // In test forEachOnZip.kt we have two declared parameters, but in fact forEach expects only one
                    parameter.replaceReturnTypeRef(
                        buildErrorTypeRef {
                            diagnostic = ConeCannotInferValueParameterType(
                                parameter.symbol, "Lambda or anonymous function has more parameters than expected"
                            )
                            source = parameter.source
                        }
                    )
                    return@forEachIndexed
                }
                val newReturnType = theParameters[index].approximateLambdaInputType(parameter.symbol, withPCLASession)
                val newReturnTypeRef = if (parameter.returnTypeRef is FirImplicitTypeRef) {
                    newReturnType.toFirResolvedTypeRef(parameter.source?.fakeElement(KtFakeSourceElementKind.ImplicitReturnTypeOfLambdaValueParameter))
                } else parameter.returnTypeRef.resolvedTypeFromPrototype(newReturnType)
                parameter.replaceReturnTypeRef(newReturnTypeRef)
                lookupTracker?.recordTypeResolveAsLookup(newReturnTypeRef, parameter.source, fileSource)
            }

            lambdaArgument.replaceValueParameters(lambdaArgument.valueParameters + listOfNotNull(itParam))
            lambdaArgument.replaceReturnTypeRef(
                expectedReturnTypeRef?.also {
                    lookupTracker?.recordTypeResolveAsLookup(it, lambdaArgument.source, fileSource)
                } ?: components.noExpectedType
            )

            var additionalConstraints: ConstraintStorage? = null

            transformer.context.withAnonymousFunctionTowerDataContext(lambdaArgument.symbol) {
                val pclaInferenceSession =
                    runIf(withPCLASession) {
                        candidate.lambdasAnalyzedWithPCLA += lambdaArgument

                        FirPCLAInferenceSession(candidate, session.inferenceComponents)
                    }

                if (pclaInferenceSession != null) {
                    transformer.context.withInferenceSession(pclaInferenceSession) {
                        lambdaArgument.transformSingle(transformer, ResolutionMode.LambdaResolution(expectedReturnTypeRef))

                        applyResultsToMainCandidate()
                    }
                } else {
                    additionalConstraints =
                        transformer.context.inferenceSession.runLambdaCompletion(candidate, forOverloadByLambdaReturnType) {
                            lambdaArgument.transformSingle(transformer, ResolutionMode.LambdaResolution(expectedReturnTypeRef))
                        }
                }
            }
            transformer.context.dropContextForAnonymousFunction(lambdaArgument)

            val returnArguments = components.dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(lambdaArgument)
                .map { ConeResolutionAtom.createRawAtom(it.expression) }

            return ReturnArgumentsAnalysisResult(returnArguments, additionalConstraints)
        }
    }

    private fun ConeKotlinType.approximateLambdaInputType(
        valueParameter: FirValueParameterSymbol?,
        isRootLambdaForPCLASession: Boolean,
    ): ConeKotlinType {
        // We only run lambda completion from ConstraintSystemCompletionContext.analyzeRemainingNotAnalyzedPostponedArgument when they are
        // left uninferred.
        // Currently, we use stub types for builder inference, so CANNOT_INFER_PARAMETER_TYPE is the only possible result here.
        if (useErrorTypeInsteadOfTypeVariableForParameterType(isReceiver = valueParameter == null, isRootLambdaForPCLASession)) {
            val diagnostic = valueParameter?.let(::ConeCannotInferValueParameterType) ?: ConeCannotInferReceiverParameterType()
            return ConeErrorType(diagnostic)
        }

        return session.typeApproximator.approximateToSuperType(
            this, TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2
        ) ?: this
    }

    private fun ConeKotlinType.useErrorTypeInsteadOfTypeVariableForParameterType(
        isReceiver: Boolean,
        isRootLambdaForPCLASession: Boolean,
    ): Boolean {
        if (this !is ConeTypeVariableType) return false

        // Receivers are expected to be fixed both for PCLA/nonPCLA lambdas, so just build error type
        if (isReceiver) return true

        // Besides PCLA, all type variables for parameter types should be fixed before lambda analysis
        // Inside PCLA (or when we start it), we force fixing receivers before lambda analysis, but allow value parameters
        // to remain unfixed TVs.
        if (isRootLambdaForPCLASession || inferenceSession is FirPCLAInferenceSession) {
            // For type variables not based on type parameters (created for lambda parameters with no expected type)
            // we force them to be fixed before lambda analysis.
            //
            // Otherwise, it's a type variable based on a type parameter which resulting type might be inferred from the lambda body,
            // so in that case leave type variable type
            return typeConstructor.originalTypeParameter == null
        }

        return true
    }
}

private fun Candidate.isFunctionForExpectTypeFromCastFeature(): Boolean {
    if (typeArgumentMapping != TypeArgumentMapping.NoExplicitArguments) return false
    val fir = symbol.fir as? FirFunction ?: return false

    return fir.isFunctionForExpectTypeFromCastFeature()
}

// Expect type is only being added to calls in a position of cast argument: foo() as R
// And that call should be resolved to something materialize()-like: it returns its single generic parameter and doesn't have value parameters
// fun <T> materialize(): T
internal fun FirFunction.isFunctionForExpectTypeFromCastFeature(): Boolean {
    val typeParameter = typeParameters.singleOrNull() ?: return false

    val returnType = returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: return false

    if ((returnType.unwrap() as? ConeTypeParameterType)?.lookupTag != typeParameter.symbol.toLookupTag()) return false

    fun FirTypeRef.isBadType() =
        coneTypeSafe<ConeKotlinType>()
            ?.contains { (it.unwrap() as? ConeTypeParameterType)?.lookupTag == typeParameter.symbol.toLookupTag() } != false

    if (valueParameters.any { it.returnTypeRef.isBadType() } || receiverParameter?.typeRef?.isBadType() == true) return false

    return true
}

private fun ConeKotlinType.unwrap(): ConeSimpleKotlinType = lowerBoundIfFlexible().let {
    if (it is ConeDefinitelyNotNullType) it.original.unwrap() else it
}
