/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirCallResolver
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.lookupTracker
import org.jetbrains.kotlin.fir.recordTypeResolveAsLookup
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedReferenceError
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeLambdaArgumentConstraintPosition
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
    val returnArguments: Collection<FirExpression>,
    val additionalConstraints: ConstraintStorage?,
)

interface LambdaAnalyzer {
    fun analyzeAndGetLambdaReturnArguments(
        lambdaAtom: ResolvedLambdaAtom,
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
        argument: PostponedResolvedAtom,
        candidate: Candidate,
        withPCLASession: Boolean,
    ) {
        when (argument) {
            is ResolvedLambdaAtom ->
                analyzeLambda(c, argument, candidate, forOverloadByLambdaReturnType = false, withPCLASession)

            is LambdaWithTypeVariableAsExpectedTypeAtom ->
                analyzeLambda(
                    c,
                    argument.transformToResolvedLambda(c.getBuilder(), resolutionContext),
                    candidate, forOverloadByLambdaReturnType = false, withPCLASession
                )

            is ResolvedCallableReferenceAtom -> processCallableReference(argument, candidate)
        }
    }

    private fun processCallableReference(atom: ResolvedCallableReferenceAtom, candidate: Candidate) {
        if (atom.mightNeedAdditionalResolution) {
            callResolver.resolveCallableReference(candidate, atom, hasSyntheticOuterCall = false)
        }

        val callableReferenceAccess = atom.reference
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
        lambda: ResolvedLambdaAtom,
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
                        ?.let { pclaInferenceSession.fixVariablesForMemberScope(it, candidate.system) }
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
        lambda: ResolvedLambdaAtom,
        candidate: Candidate,
        results: ReturnArgumentsAnalysisResult,
        substitute: (ConeKotlinType) -> ConeKotlinType = c.createSubstituteFunctorForLambdaAnalysis(),
    ) {
        val (returnArguments, additionalConstraintStorage) = results

        if (additionalConstraintStorage != null) {
            c.addOtherSystem(additionalConstraintStorage)
        }

        val checkerSink: CheckerSink = CheckerSinkImpl(candidate)
        val builder = c.getBuilder()

        val lastExpression = lambda.atom.body?.statements?.lastOrNull() as? FirExpression
        var hasExpressionInReturnArguments = false
        val returnTypeRef = lambda.atom.returnTypeRef.let {
            it as? FirResolvedTypeRef ?: it.resolvedTypeFromPrototype(substitute(lambda.returnType))
        }
        val lambdaExpectedTypeIsUnit = returnTypeRef.type.isUnitOrFlexibleUnit
        returnArguments.forEach {
            // If the lambda returns Unit, the last expression is not returned and should not be constrained.
            val isLastExpression = it == lastExpression

            // TODO (KT-55837) questionable moment inherited from FE1.0 (the `haveSubsystem` case):
            //    fun <T> foo(): T
            //    run {
            //      if (p) return@run
            //      foo() // T = Unit, even though there is no implicit return
            //    }
            //  Things get even weirder if T has an upper bound incompatible with Unit.
            val haveSubsystem = c.addSubsystemFromExpression(it)
            val isUnitLambda = lambdaExpectedTypeIsUnit || lambda.atom.shouldReturnUnit(returnArguments)
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
                        it.resolvedType, returnTypeRef.type,
                        ConeLambdaArgumentConstraintPosition(lambda.atom)
                    )
                }
                return@forEach
            }

            hasExpressionInReturnArguments = true
            if (!builder.hasContradiction) {
                candidate.resolveArgumentExpression(
                    builder,
                    it,
                    returnTypeRef.type,
                    checkerSink,
                    context = resolutionContext,
                    isReceiver = false,
                    isDispatch = false
                )
            }
        }

        if (!hasExpressionInReturnArguments) {
            builder.addSubtypeConstraint(
                components.session.builtinTypes.unitType.type,
                substitute(lambda.returnType),
                ConeLambdaArgumentConstraintPosition(lambda.atom)
            )
        }

        lambda.analyzed = true
        lambda.returnStatements = returnArguments
        c.resolveForkPointsConstraints()
    }

    fun PostponedArgumentsAnalyzerContext.createSubstituteFunctorForLambdaAnalysis(): (ConeKotlinType) -> ConeKotlinType {
        val stubsForPostponedVariables = bindingStubsForPostponedVariables()
        val currentSubstitutor = buildCurrentSubstitutor(stubsForPostponedVariables.mapKeys { it.key.freshTypeConstructor(this) })
        return { currentSubstitutor.safeSubstitute(this, it) as ConeKotlinType }
    }
}

fun LambdaWithTypeVariableAsExpectedTypeAtom.transformToResolvedLambda(
    csBuilder: ConstraintSystemBuilder,
    context: ResolutionContext,
    expectedType: ConeKotlinType? = null,
    returnTypeVariable: ConeTypeVariableForLambdaReturnType? = null,
): ResolvedLambdaAtom {
    val fixedExpectedType = (csBuilder.buildCurrentSubstitutor() as ConeSubstitutor)
        .substituteOrSelf(expectedType ?: this.expectedType)
    val resolvedAtom = candidateOfOuterCall.preprocessLambdaArgument(
        csBuilder,
        atom,
        fixedExpectedType,
        context,
        sink = null,
        duringCompletion = true,
        returnTypeVariable = returnTypeVariable
    ) as ResolvedLambdaAtom
    analyzed = true
    return resolvedAtom
}
