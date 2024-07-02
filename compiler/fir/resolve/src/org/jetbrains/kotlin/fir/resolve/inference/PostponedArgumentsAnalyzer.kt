/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.expressions.FirExpression
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
import org.jetbrains.kotlin.fir.resolve.shouldReturnUnit
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
        contextReceivers: List<ConeKotlinType>,
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
        if (atom.mightNeedAdditionalResolution) {
            callResolver.resolveCallableReference(candidate, atom, hasSyntheticOuterCall = false)
        }

        val callableReferenceAccess = atom.fir
        atom.analyzed = true

        resolutionContext.bodyResolveContext.dropCallableReferenceContext(callableReferenceAccess)

        val namedReference = atom.resultingReference ?: buildErrorNamedReference {
            source = callableReferenceAccess.source
            diagnostic = ConeUnresolvedReferenceError(callableReferenceAccess.calleeReference.name)
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

        val additionalBindings: Map<TypeConstructorMarker, KotlinTypeMarker>? =
            (resolutionContext.bodyResolveContext.inferenceSession as? FirPCLAInferenceSession)?.let { pclaInferenceSession ->
                // TODO: Fix variables for context receivers, too (KT-64859)
                buildMap {
                    lambda.receiver
                        ?.let { pclaInferenceSession.fixCurrentResultIfTypeVariableAndReturnBinding(it, candidate.system) }
                        ?.let(this::plusAssign)
                }
            }

        val unitType = components.session.builtinTypes.unitType.type
        val currentSubstitutor = c.buildCurrentSubstitutor(additionalBindings ?: emptyMap()) as ConeSubstitutor

        fun substitute(type: ConeKotlinType) = currentSubstitutor.safeSubstitute(c, type) as ConeKotlinType

        val receiver = lambda.receiver?.let(::substitute)
        val contextReceivers = lambda.contextReceivers.map(::substitute)
        val parameters = lambda.parameters.map(::substitute)
        val rawReturnType = lambda.returnType

        val expectedTypeForReturnArguments = when {
            c.canBeProper(rawReturnType) -> substitute(rawReturnType)

            // For Unit-coercion
            !rawReturnType.isMarkedNullable && c.hasUpperOrEqualUnitConstraint(rawReturnType) -> unitType

            else -> null
        }

        val results = lambdaAnalyzer.analyzeAndGetLambdaReturnArguments(
            lambda,
            receiver,
            contextReceivers,
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

        val lastExpression = lambda.fir.lastStatement() as? FirExpression
        var hasExpressionInReturnArguments = false
        val returnTypeRef = lambda.fir.returnTypeRef.let {
            it as? FirResolvedTypeRef ?: it.resolvedTypeFromPrototype(substituteAlreadyFixedVariables(lambda.returnType))
        }
        val isUnitLambda = returnTypeRef.type.isUnitOrFlexibleUnit || lambda.fir.shouldReturnUnit(returnArguments)

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
            val haveSubsystem = c.addSubsystemFromExpression(expression)
            if (isLastExpression && isUnitLambda) {
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
                        expression.resolvedType, returnTypeRef.type,
                        ConeLambdaArgumentConstraintPosition(lambda.fir)
                    )
                }
                continue
            }

            hasExpressionInReturnArguments = true
            if (!builder.hasContradiction) {
                ArgumentCheckingProcessor.resolveArgumentExpression(
                    candidate,
                    atom,
                    substituteAlreadyFixedVariables(lambda.returnType),
                    checkerSink,
                    context = resolutionContext,
                    isReceiver = false,
                    isDispatch = false
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

        val position = ConeLambdaArgumentConstraintPosition(lambda.fir)
        val unitType = components.session.builtinTypes.unitType.type
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
                        lambda.fir,
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
