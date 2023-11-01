/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.freshTypeConstructor
import org.jetbrains.kotlin.types.model.safeSubstitute

data class ReturnArgumentsAnalysisResult(
    val returnArguments: Collection<FirExpression>,
    val inferenceSession: FirInferenceSession?,
)

interface LambdaAnalyzer {
    fun analyzeAndGetLambdaReturnArguments(
        lambdaAtom: ResolvedLambdaAtom,
        receiverType: ConeKotlinType?,
        contextReceivers: List<ConeKotlinType>,
        parameters: List<ConeKotlinType>,
        expectedReturnType: ConeKotlinType?, // null means, that return type is not proper i.e. it depends on some type variables
        candidate: Candidate,
        notFixedTypeVariablesInInputTypes: Set<TypeConstructorMarker>,
        currentSubstitutor: ConeSubstitutor,
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
    ) {
        when (argument) {
            is ResolvedLambdaAtom ->
                analyzeLambda(c, argument, candidate)

            is LambdaWithTypeVariableAsExpectedTypeAtom ->
                analyzeLambda(c, argument.transformToResolvedLambda(c.getBuilder(), resolutionContext), candidate)

            is ResolvedCallableReferenceAtom -> processCallableReference(argument, candidate)
        }
    }

    private fun processCallableReference(atom: ResolvedCallableReferenceAtom, candidate: Candidate) {
        if (atom.mightNeedAdditionalResolution) {
            callResolver.resolveCallableReference(candidate.csBuilder, atom)
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
        //diagnosticHolder: KotlinDiagnosticsHolder
    ): ReturnArgumentsAnalysisResult {
        // TODO: replace with `require(!lambda.analyzed)` when KT-54767 will be fixed
        if (lambda.analyzed) {
            return ReturnArgumentsAnalysisResult(lambda.returnStatements, inferenceSession = null)
        }

        val additionalBindings: Map<TypeConstructorMarker, KotlinTypeMarker>? =
            (resolutionContext.bodyResolveContext.inferenceSession as? FirBuilderInferenceSession2)?.let { _ ->
                // TODO: context receivers
                buildMap {
//                    lambda.receiver
//                        ?.let { builderInferenceSession.fixVariablesForMemberScope(it, candidate.system) }
//                        ?.let(this::plusAssign)
//
//                    for (parameterType in lambda.parameters) {
//                        builderInferenceSession.fixVariablesForMemberScope(parameterType, candidate.system)?.let(this::plusAssign)
//                    }
                }
            }

        val unitType = components.session.builtinTypes.unitType.type
        val currentSubstitutor = c.buildCurrentSubstitutor(additionalBindings ?: emptyMap()) as ConeSubstitutor

        fun substitute(type: ConeKotlinType) = (currentSubstitutor.safeSubstitute(c, type) as ConeKotlinType).independentInstance()

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

        val notFixedTypeVariablesInInputTypes =
            lambda.inputTypes
                .flatMap {
                    with(c) { it.extractTypeVariables() }
                }.filterTo(mutableSetOf()) { it in c.notFixedTypeVariables }

        val results = lambdaAnalyzer.analyzeAndGetLambdaReturnArguments(
            lambda,
            receiver,
            contextReceivers,
            parameters,
            expectedTypeForReturnArguments,
            candidate,
            notFixedTypeVariablesInInputTypes,
            currentSubstitutor,
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
        val (returnArguments, _) = results

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
            // Not calling `addSubsystemFromExpression` for builder-inference is crucial
            c.addSubsystemFromExpression(it)
            if (isLastExpression &&
                (lambdaExpectedTypeIsUnit || lambda.atom.shouldReturnUnit(returnArguments))
            ) return@forEach

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

        if (!hasExpressionInReturnArguments && !lambdaExpectedTypeIsUnit) {
            builder.addSubtypeConstraint(
                components.session.builtinTypes.unitType.type,
                returnTypeRef.type,
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
