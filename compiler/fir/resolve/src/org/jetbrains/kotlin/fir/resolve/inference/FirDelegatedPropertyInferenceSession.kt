/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCallOrigin
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.InferenceError
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeVariableTypeConstructor
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.util.OperatorNameConventions

class FirDelegatedPropertyInferenceSession(
    val property: FirProperty,
    resolutionContext: ResolutionContext,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
) : FirInferenceSessionForChainedResolve(resolutionContext) {

    private var currentConstraintSystem = components.session.inferenceComponents.createConstraintSystem()
    val currentConstraintStorage: ConstraintStorage get() = currentConstraintSystem.currentStorage()

    private val unitType: ConeClassLikeType = components.session.builtinTypes.unitType.type

    // TODO after PCLA (KT-59107):
    //  Outer system seems to be a property of a concrete call resolution and probably should be applied to concrete call resolution
    override fun <R> onCandidatesResolution(call: FirFunctionCall, candidatesResolutionCallback: () -> R): R {
        return if (!call.isAnyOfDelegateOperators())
            candidatesResolutionCallback()
        else
            resolutionContext.bodyResolveContext.withOuterConstraintStorage(
                currentConstraintSystem.currentStorage(),
                candidatesResolutionCallback
            )
    }

    override fun <T> shouldAvoidFullCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement =
        call.isAnyOfDelegateOperators()

    override fun <T> processPartiallyResolvedCall(call: T, resolutionMode: ResolutionMode) where T : FirResolvable, T : FirStatement {
        if (resolutionMode != ResolutionMode.ContextDependent.Delegate && !call.isAnyOfDelegateOperators()) return

        val candidate = call.candidate

        // Ignore unsuccessful `provideDelegate` candidates
        // This behavior is aligned with a relevant part at FirDeclarationsResolveTransformer.transformWrappedDelegateExpression
        if (call.isProvideDelegate() && !candidate.isSuccessful) return

        val candidateSystem = candidate.system

        partiallyResolvedCalls.add(call to candidate)
        currentConstraintSystem = candidateSystem
    }

    private fun <T> T.isProvideDelegate() where T : FirResolvable, T : FirStatement =
        isAnyOfDelegateOperators() && (this as FirFunctionCall).calleeReference.name == OperatorNameConventions.PROVIDE_DELEGATE

    private fun <T> T.isAnyOfDelegateOperators(): Boolean where T : FirResolvable {
        if (this !is FirFunctionCall || origin != FirFunctionCallOrigin.Operator) return false
        val name = calleeReference.name
        return name == OperatorNameConventions.PROVIDE_DELEGATE || name == OperatorNameConventions.GET_VALUE || name == OperatorNameConventions.SET_VALUE
    }

    override fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        constraintSystemBuilder: ConstraintSystemBuilder,
        completionMode: ConstraintSystemCompletionMode
    ): Map<ConeTypeVariableTypeConstructor, ConeKotlinType>? = null

    fun completeCandidates(): List<FirResolvable> {
        val commonSystem = currentConstraintSystem.apply { prepareForGlobalCompletion() }

        val notCompletedCalls = partiallyResolvedCalls.mapNotNull { partiallyResolvedCall ->
            partiallyResolvedCall.first.takeIf { resolvable ->
                resolvable.candidate() != null
            }
        }

        resolutionContext.bodyResolveContext.withInferenceSession(DEFAULT) {
            @Suppress("UNCHECKED_CAST")
            components.callCompleter.completer.complete(
                commonSystem.asConstraintSystemCompleterContext(),
                ConstraintSystemCompletionMode.FULL,
                notCompletedCalls as List<FirStatement>,
                unitType, resolutionContext
            ) { lambdaAtom ->
                // Reversed here bc we want top-most call to avoid exponential visit
                val containingCandidateForLambda = notCompletedCalls.asReversed().first {
                    var found = false
                    it.processAllContainingCallCandidates(processBlocks = true) { subCandidate ->
                        if (subCandidate.postponedAtoms.contains(lambdaAtom)) {
                            found = true
                        }
                    }
                    found
                }.candidate
                postponedArgumentsAnalyzer.analyze(
                    commonSystem,
                    lambdaAtom,
                    containingCandidateForLambda,
                    ConstraintSystemCompletionMode.FULL,
                )
            }
        }

        for ((_, candidate) in partiallyResolvedCalls) {
            for (error in commonSystem.errors) {
                candidate.addDiagnostic(InferenceError(error))
            }
        }

        return notCompletedCalls
    }

    fun createFinalSubstitutor(): ConeSubstitutor =
        currentConstraintSystem.asReadOnlyStorage()
            .buildAbstractResultingSubstitutor(components.session.typeContext) as ConeSubstitutor

    override fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement {}

    override fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement = true
}
