/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.lookupTracker
import org.jetbrains.kotlin.fir.recordTypeResolveAsLookup
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate.*
import org.jetbrains.kotlin.fir.resolve.calls.stages.ArgumentCheckingProcessor
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.lastStatement
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedReferenceError
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeLambdaArgumentConstraintPosition
import org.jetbrains.kotlin.fir.resolve.isImplicitUnitForEmptyLambda
import org.jetbrains.kotlin.fir.resolve.lambdaWithExplicitEmptyReturns
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.freshTypeConstructor
import org.jetbrains.kotlin.types.model.safeSubstitute

data class ReturnArgumentsAnalysisResult(
    val returnArguments: Collection<ConeResolutionAtom>,
    val additionalConstraints: ConstraintStorage?,
)

interface LambdaAnalyzer {
    fun analyzeAndGetLambdaReturnArguments(
        lambdaAtom: ConeResolvedLambdaAtom,
        receiverType: ConeKotlinType?,
        contextParameters: List<ConeKotlinType>,
        parameters: List<ConeKotlinType>,
        expectedReturnType: ConeKotlinType?, // null means, that return type is not proper i.e. it depends on some type variables
        candidate: Candidate,
        withPCLASession: Boolean,
        forOverloadByLambdaReturnType: Boolean,
    ): ReturnArgumentsAnalysisResult
}

class PostponedArgumentsAnalyzer(
    private val resolutionContext: ResolutionContext,
    private val lambdaAnalyzer: LambdaAnalyzer,
    private val components: InferenceComponents,
    private val callResolver: FirCallResolver,
) {

    fun analyze(
        c: PostponedArgumentsAnalyzerContext,
        argument: ConePostponedResolvedAtom,
        candidate: Candidate,
        withPCLASession: Boolean,
    ) {
        when (argument) {
            is ConeResolvedLambdaAtom ->
                analyzeLambda(c, argument, candidate, forOverloadByLambdaReturnType = false, withPCLASession)

            is ConeLambdaWithTypeVariableAsExpectedTypeAtom ->
                analyzeLambda(
                    c,
                    argument.transformToResolvedLambda(c.getBuilder(), resolutionContext),
                    candidate, forOverloadByLambdaReturnType = false, withPCLASession
                )

            is ConeResolvedCallableReferenceAtom -> processCallableReference(argument, candidate)
        }
    }

    private fun processCallableReference(atom: ConeResolvedCallableReferenceAtom, candidate: Candidate) {
        if (atom.needsResolution) {
            // Needed only for the assertion below
            val stateBeforeResolution = atom.state

            callResolver.resolveCallableReference(candidate, atom, hasSyntheticOuterCall = false)

            if (atom.isPostponedBecauseOfAmbiguity
                && candidate.callInfo.session.languageVersionSettings.supportsFeature(
                    LanguageFeature.CallableReferenceOverloadResolutionInLambda
                )
            ) {
                // If the current state is POSTPONED_BECAUSE_OF_AMBIGUITY, the previous might be only NOT_RESOLVED_YET
                // That effectively means that it's not `foo(::bar)` case and neither `::foo` in the air because for them,
                // we would resolve it once at `EagerResolveOfCallableReferences` stage for the containing call.
                check(stateBeforeResolution == ConeResolvedCallableReferenceAtom.State.NOT_RESOLVED_YET)

                // Here, it's very likely the case like `foo { :::bar }` where we look at the `::bar` as a new atom which might
                // be resolved at any time as it has empty `inputTypes` and `outputTypes` dependencies
                //  (see ConeResolvedCallableReferenceAtom.inputTypes).
                //
                // So, the idea is to leave the atom postponed and to finalize it until `inputTypes` are ready.
                //
                // See similar code in K1
                // at org.jetbrains.kotlin.resolve.calls.components.CallableReferenceArgumentResolver.processCallableReferenceArgument
                return
            }
        }

        // TODO: Consider moving this part to FirCallResolver::resolveCallableReference (KT-74021)
        // Currently it doesn't work easily because the code inside
        // FirSyntheticCallGenerator.resolveCallableReferenceWithSyntheticOuterCall for error processing assumes
        // that the reference is not replaced
        // (see `check(callableReferenceAccess.calleeReference is FirSimpleNamedReference && !callableReferenceAccess.isResolved)`).
        // But generally, it should help to get rid of `analyzed` var and replace it with
        // getter to `ConeResolvedCallableReferenceAtom::state`.
        val callableReferenceAccess = atom.expression
        atom.analyzed = true

        resolutionContext.bodyResolveContext.dropCallableReferenceContext(callableReferenceAccess)

        val namedReference = atom.resultingReference ?: buildErrorNamedReference {
            source = callableReferenceAccess.source
            diagnostic = ConeUnresolvedReferenceError(callableReferenceAccess.calleeReference.name)
            name = callableReferenceAccess.calleeReference.name
        }

        callableReferenceAccess.apply {
            replaceCalleeReference(namedReference)
            val typeForCallableReference = atom.resultingTypeForCallableReference
            val resolvedType = when {
                typeForCallableReference != null -> typeForCallableReference
                namedReference is FirErrorReferenceWithCandidate -> ConeErrorType(namedReference.diagnostic)
                else -> ConeErrorType(ConeUnresolvedReferenceError(callableReferenceAccess.calleeReference.name))
            }
            replaceConeTypeOrNull(resolvedType)
            resolutionContext.session.lookupTracker?.recordTypeResolveAsLookup(
                resolvedType, source, resolutionContext.bodyResolveComponents.file.source
            )
        }
    }

    fun analyzeLambda(
        c: PostponedArgumentsAnalyzerContext,
        lambda: ConeResolvedLambdaAtom,
        candidate: Candidate,
        forOverloadByLambdaReturnType: Boolean,
        withPCLASession: Boolean,
        //diagnosticHolder: KotlinDiagnosticsHolder
    ): ReturnArgumentsAnalysisResult {
        // TODO: replace with `require(!lambda.analyzed)` when KT-54767 will be fixed
        if (lambda.analyzed) {
            return ReturnArgumentsAnalysisResult(lambda.returnStatements, additionalConstraints = null)
        }

        val additionalBinding: Pair<TypeConstructorMarker, KotlinTypeMarker>? =
            (resolutionContext.bodyResolveContext.inferenceSession as? FirPCLAInferenceSession)?.let { pclaInferenceSession ->
                // TODO: Fix variables for context receivers, too (KT-64859)
                lambda.receiverType
                    ?.let { pclaInferenceSession.semiFixCurrentResultIfTypeVariableAndReturnBinding(it, candidate.system) }
            }

        val unitType = components.session.builtinTypes.unitType.coneType
        val currentSubstitutor = c.buildCurrentSubstitutor(additionalBinding) as ConeSubstitutor

        fun substitute(type: ConeKotlinType) = currentSubstitutor.safeSubstitute(c, type) as ConeKotlinType

        val receiver = lambda.receiverType?.let(::substitute)
        val contextParameters = lambda.contextParameterTypes.map(::substitute)
        val parameters = lambda.parameterTypes.map(::substitute)
        val lambdaReturnType = lambda.returnType

        val expectedTypeForReturnArguments = when {
            c.canBeProper(lambdaReturnType) -> substitute(lambdaReturnType)

            // For Unit-coercion
            !lambdaReturnType.isMarkedNullable && c.hasUpperOrEqualUnitConstraint(lambdaReturnType) -> unitType

            // Supplying the expected type for lambda effectively makes it being resolved in the FULL completion mode.
            // For non-PCLA lambdas using expected types with non-fixed type variables would lead to illegal state: calls inside return
            // statements are not aware of type variables of the "main" call.
            // But for PCLA, we resolve everything within a common CS; thus it's ok.
            //
            // The main purpose of this condition is actually forcing lambda analysis in return statements, so we might gather
            // constraints for the builder-related type variable from the nested lambdas.
            //
            // For more details, see #analysis-mode-for-return-statements-of-a-pcla-lambda at [docs/fir/pcla.md]
            //
            // NB: It's explicitly put below the unit case
            // (see testData/diagnostics/tests/inference/pcla/lambdaBelongsToOuterCallUnitConstraint.kt)
            withPCLASession && resolutionContext.session.languageVersionSettings.supportsFeature(LanguageFeature.PCLAEnhancementsIn21) ->
                substitute(lambdaReturnType)

            else -> null
        }

        val results = lambdaAnalyzer.analyzeAndGetLambdaReturnArguments(
            lambda,
            receiver,
            contextParameters,
            parameters,
            expectedTypeForReturnArguments,
            candidate,
            withPCLASession,
            forOverloadByLambdaReturnType,
        )
        applyResultsOfAnalyzedLambdaToCandidateSystem(
            c,
            lambda,
            candidate,
            results,
            ::substitute
        )
        return results
    }

    fun applyResultsOfAnalyzedLambdaToCandidateSystem(
        c: PostponedArgumentsAnalyzerContext,
        lambda: ConeResolvedLambdaAtom,
        candidate: Candidate,
        results: ReturnArgumentsAnalysisResult,
        substituteAlreadyFixedVariables: (ConeKotlinType) -> ConeKotlinType = c.createSubstituteFunctorForLambdaAnalysis(),
    ) {
        val (returnAtoms, additionalConstraintStorage) = results
        val returnArguments = returnAtoms.map { it.expression }

        if (additionalConstraintStorage != null) {
            c.addOtherSystem(additionalConstraintStorage)
        }

        val checkerSink: CheckerSink = CheckerSinkImpl(candidate)
        val builder = c.getBuilder()

        val lastExpression = lambda.anonymousFunction.lastStatement() as? FirExpression
        var hasExpressionInReturnArguments = false
        val returnTypeRef = lambda.anonymousFunction.returnTypeRef.let {
            it as? FirResolvedTypeRef ?: it.resolvedTypeFromPrototype(
                substituteAlreadyFixedVariables(lambda.returnType),
                lambda.anonymousFunction.source?.fakeElement(KtFakeSourceElementKind.ImplicitFunctionReturnType)
            )
        }
        val isLastExpressionCoercedToUnit =
            returnTypeRef.coneType.isUnitOrFlexibleUnit || lambda.anonymousFunction.lambdaWithExplicitEmptyReturns(returnArguments)

        for (atom in returnAtoms) {
            val expression = atom.expression
            if (expression.isImplicitUnitForEmptyLambda()) continue
            // If the lambda returns Unit, the last expression is not returned and should not be constrained.
            val isLastExpression = expression == lastExpression

            // TODO (KT-55837) questionable moment inherited from FE1.0 (the `haveSubsystem` case):
            //    fun <T> foo(): T
            //    run {
            //      if (p) return@run
            //      foo() // T = Unit, even though there is no implicit return
            //    }
            //  Things get even weirder if T has an upper bound incompatible with Unit.
            val haveSubsystem = c.addSubsystemFromAtom(atom)
            if (isLastExpression && isLastExpressionCoercedToUnit) {
                // That "if" is necessary because otherwise we would force a lambda return type
                // to be inferred from completed last expression.
                // See `test1` at testData/diagnostics/tests/inference/coercionToUnit/afterBareReturn.kt
                if (haveSubsystem) {
                    // We don't force it because of the cases like
                    // buildMap {
                    //    put("a", 1) // While `put` returns V, we should not enforce the latter to be a subtype of Unit
                    // }
                    // See KT-63602 for details.
                    builder.addSubtypeConstraintIfCompatible(
                        expression.resolvedType, returnTypeRef.coneType,
                        ConeLambdaArgumentConstraintPosition(lambda.anonymousFunction)
                    )
                }
                continue
            }

            hasExpressionInReturnArguments = true
            // Nested lambdas need to be resolved even when we have a contradiction.
            if (!builder.hasContradiction || atom is ConeResolutionAtomWithPostponedChild) {
                ArgumentCheckingProcessor.resolveArgumentExpression(
                    candidate,
                    atom,
                    substituteAlreadyFixedVariables(lambda.returnType),
                    checkerSink,
                    context = resolutionContext,
                    isReceiver = false,
                    isDispatch = false,
                    anonymousFunctionIfReturnExpression = lambda.anonymousFunction,
                )
            }
        }

        if (!hasExpressionInReturnArguments) {
            addLambdaReturnTypeUnitConstraintOrReportError(c, builder, lambda, checkerSink, substituteAlreadyFixedVariables)
        }

        lambda.analyzed = true
        lambda.returnStatements = returnAtoms
    }

    private fun addLambdaReturnTypeUnitConstraintOrReportError(
        c: PostponedArgumentsAnalyzerContext,
        builder: ConstraintSystemBuilder,
        lambda: ConeResolvedLambdaAtom,
        checkerSink: CheckerSink,
        substituteAlreadyFixedVariables: (ConeKotlinType) -> ConeKotlinType,
    ) {
        val lambdaReturnType = substituteAlreadyFixedVariables(lambda.returnType)

        // If we've got some errors already, no new constraints or diagnostics are required
        if (with(c) { lambdaReturnType.isError() } || builder.hasContradiction) return

        val position = ConeLambdaArgumentConstraintPosition(lambda.anonymousFunction)
        val unitType = components.session.builtinTypes.unitType.coneType
        if (!builder.addSubtypeConstraintIfCompatible(
                unitType,
                lambdaReturnType,
                position
            )
        ) {
            val wholeLambdaExpectedType =
                lambda.expectedType?.let { substituteAlreadyFixedVariables(it) }

            if (wholeLambdaExpectedType != null) {
                checkerSink.reportDiagnostic(
                    // TODO: Consider replacement with ArgumentTypeMismatch once KT-67961 is fixed
                    // Currently, ArgumentTypeMismatch only allows expressions and we don't have it here
                    UnitReturnTypeLambdaContradictsExpectedType(
                        lambda.anonymousFunction,
                        wholeLambdaExpectedType,
                        lambda.sourceForFunctionExpression,
                    )
                )
            } else {
                // Fallback situation, probably quite rare or even impossible, though it's hard to proof that.
                // But we're still forcing some constraint error, not to leave the candidate falsely successful.
                builder.addSubtypeConstraint(
                    unitType,
                    lambdaReturnType,
                    position,
                )
            }
        }
    }

    private fun PostponedArgumentsAnalyzerContext.createSubstituteFunctorForLambdaAnalysis(): (ConeKotlinType) -> ConeKotlinType {
        val stubsForPostponedVariables = bindingStubsForPostponedVariables()
        val currentSubstitutor = buildCurrentSubstitutor(stubsForPostponedVariables.mapKeys { it.key.freshTypeConstructor(this) })
        return { currentSubstitutor.safeSubstitute(this, it) as ConeKotlinType }
    }
}

fun ConeLambdaWithTypeVariableAsExpectedTypeAtom.transformToResolvedLambda(
    csBuilder: ConstraintSystemBuilder,
    context: ResolutionContext,
    expectedType: ConeKotlinType? = null,
    returnTypeVariable: ConeTypeVariableForLambdaReturnType? = null,
): ConeResolvedLambdaAtom {
    val fixedExpectedType = (csBuilder.buildCurrentSubstitutor() as ConeSubstitutor)
        .substituteOrSelf(expectedType ?: this.expectedType)
    val resolvedAtom = ArgumentCheckingProcessor.createResolvedLambdaAtomDuringCompletion(
        candidateOfOuterCall, csBuilder, ConeResolutionAtomWithPostponedChild(expression), fixedExpectedType,
        context, returnTypeVariable = returnTypeVariable
    )

    subAtom = resolvedAtom
    analyzed = true

    return resolvedAtom
}
