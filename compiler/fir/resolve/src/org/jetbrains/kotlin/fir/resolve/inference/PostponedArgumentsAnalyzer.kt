/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirCallResolver
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.lookupTracker
import org.jetbrains.kotlin.fir.recordTypeResolveAsLookup
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedReferenceError
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeLambdaArgumentConstraintPosition
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.StoreNameReference
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.model.CoroutinePosition
import org.jetbrains.kotlin.types.model.StubTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.types.model.freshTypeConstructor
import org.jetbrains.kotlin.types.model.safeSubstitute

data class ReturnArgumentsAnalysisResult(
    val returnArguments: Collection<FirStatement>,
    val inferenceSession: FirInferenceSession?
)

interface LambdaAnalyzer {
    fun analyzeAndGetLambdaReturnArguments(
        lambdaAtom: ResolvedLambdaAtom,
        receiverType: ConeKotlinType?,
        parameters: List<ConeKotlinType>,
        expectedReturnType: ConeKotlinType?, // null means, that return type is not proper i.e. it depends on some type variables
        stubsForPostponedVariables: Map<TypeVariableMarker, StubTypeMarker>
    ): ReturnArgumentsAnalysisResult
}

class PostponedArgumentsAnalyzer(
    private val resolutionContext: ResolutionContext,
    private val lambdaAnalyzer: LambdaAnalyzer,
    private val components: InferenceComponents,
    private val callResolver: FirCallResolver
) {

    fun analyze(
        c: PostponedArgumentsAnalyzerContext,
        argument: PostponedResolvedAtom,
        candidate: Candidate
    ) {
        when (argument) {
            is ResolvedLambdaAtom ->
                analyzeLambda(c, argument, candidate)

            is LambdaWithTypeVariableAsExpectedTypeAtom ->
                analyzeLambda(c, argument.transformToResolvedLambda(c.getBuilder(), resolutionContext), candidate)

            is ResolvedCallableReferenceAtom -> processCallableReference(argument, candidate)

//            is ResolvedCollectionLiteralAtom -> TODO("Not supported")
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

        callableReferenceAccess.transformCalleeReference(
            StoreNameReference,
            namedReference
        ).apply {
            val typeForCallableReference = atom.resultingTypeForCallableReference
            val resolvedTypeRef = when {
                typeForCallableReference != null -> buildResolvedTypeRef {
                    type = typeForCallableReference
                }
                namedReference is FirErrorReferenceWithCandidate -> buildErrorTypeRef {
                    diagnostic = namedReference.diagnostic
                }
                else -> buildErrorTypeRef {
                    diagnostic = ConeUnresolvedReferenceError(callableReferenceAccess.calleeReference.name)
                }
            }
            replaceTypeRef(resolvedTypeRef)
            resolutionContext.session.lookupTracker?.recordTypeResolveAsLookup(resolvedTypeRef, source, null)
        }
    }

    fun analyzeLambda(
        c: PostponedArgumentsAnalyzerContext,
        lambda: ResolvedLambdaAtom,
        candidate: Candidate
        //diagnosticHolder: KotlinDiagnosticsHolder
    ): ReturnArgumentsAnalysisResult {
        val unitType = components.session.builtinTypes.unitType.type
        val stubsForPostponedVariables = c.bindingStubsForPostponedVariables()
        val currentSubstitutor = c.buildCurrentSubstitutor(stubsForPostponedVariables.mapKeys { it.key.freshTypeConstructor(c) })

        fun substitute(type: ConeKotlinType) = currentSubstitutor.safeSubstitute(c, type) as ConeKotlinType

        val receiver = lambda.receiver?.let(::substitute)
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
            parameters,
            expectedTypeForReturnArguments,
            stubsForPostponedVariables
        )
        applyResultsOfAnalyzedLambdaToCandidateSystem(c, lambda, candidate, results, expectedTypeForReturnArguments, ::substitute)
        return results
    }

    fun applyResultsOfAnalyzedLambdaToCandidateSystem(
        c: PostponedArgumentsAnalyzerContext,
        lambda: ResolvedLambdaAtom,
        candidate: Candidate,
        results: ReturnArgumentsAnalysisResult,
        expectedReturnType: ConeKotlinType? = null,
        substitute: (ConeKotlinType) -> ConeKotlinType = c.createSubstituteFunctorForLambdaAnalysis()
    ) {
        val (returnArguments, inferenceSession) = results
        if (inferenceSession != null) {
            val storageSnapshot = c.getBuilder().currentStorage()

            val postponedVariables = inferenceSession.inferPostponedVariables(lambda, storageSnapshot)

            if (postponedVariables == null) {
                c.getBuilder().removePostponedVariables()
            } else {
                for ((constructor, resultType) in postponedVariables) {
                    val variableWithConstraints = storageSnapshot.notFixedTypeVariables[constructor] ?: continue
                    val variable = variableWithConstraints.typeVariable as ConeTypeVariable

                    c.getBuilder().unmarkPostponedVariable(variable)
                    c.getBuilder().addEqualityConstraint(variable.defaultType, resultType, CoroutinePosition)
                }
            }
        }

        returnArguments.forEach { c.addSubsystemFromExpression(it) }

        val checkerSink: CheckerSink = CheckerSinkImpl(candidate)

        val lastExpression = lambda.atom.body?.statements?.lastOrNull() as? FirExpression
        var hasExpressionInReturnArguments = false
        // No constraint for return expressions of lambda if it has Unit return type.
        val lambdaReturnType = lambda.returnType.let(substitute).takeUnless { it.isUnitOrFlexibleUnit }
        returnArguments.forEach {
            if (it !is FirExpression) return@forEach
            hasExpressionInReturnArguments = true
            // If it is the last expression, and the expected type is Unit, that expression will be coerced to Unit.
            // If the last expression is of Unit type, of course it's not coercion-to-Unit case.
            val lastExpressionCoercedToUnit =
                it == lastExpression && expectedReturnType?.isUnitOrFlexibleUnit == true && !it.typeRef.coneType.isUnitOrFlexibleUnit
            // No constraint for the last expression of lambda if it will be coerced to Unit.
            if (!lastExpressionCoercedToUnit && !c.getBuilder().hasContradiction) {
                candidate.resolveArgumentExpression(
                    c.getBuilder(),
                    it,
                    lambdaReturnType,
                    lambda.atom.returnTypeRef, // TODO: proper ref
                    checkerSink,
                    context = resolutionContext,
                    isReceiver = false,
                    isDispatch = false
                )
            }
        }

        if (!hasExpressionInReturnArguments && lambdaReturnType != null) {
            /*LambdaArgumentConstraintPosition(lambda)*/
            c.getBuilder().addEqualityConstraint(
                lambdaReturnType,
                components.session.builtinTypes.unitType.type,
                ConeLambdaArgumentConstraintPosition(lambda.atom)
            )
        }

        lambda.analyzed = true
        lambda.returnStatements = returnArguments
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
    returnTypeVariable: ConeTypeVariableForLambdaReturnType? = null
): ResolvedLambdaAtom {
    val fixedExpectedType = (csBuilder.buildCurrentSubstitutor() as ConeSubstitutor)
        .substituteOrSelf(expectedType ?: this.expectedType)
    val resolvedAtom = candidateOfOuterCall.preprocessLambdaArgument(
        csBuilder,
        atom,
        fixedExpectedType,
        expectedTypeRef,
        context,
        sink = null,
        duringCompletion = true,
        returnTypeVariable = returnTypeVariable
    ) as ResolvedLambdaAtom
    analyzed = true
    return resolvedAtom
}
