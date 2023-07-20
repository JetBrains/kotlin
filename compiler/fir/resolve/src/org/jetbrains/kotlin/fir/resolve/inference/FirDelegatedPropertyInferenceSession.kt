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
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.util.OperatorNameConventions

class FirDelegatedPropertyInferenceSession(
    val property: FirProperty,
    resolutionContext: ResolutionContext,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
) : FirInferenceSessionForChainedResolve(resolutionContext) {

    var currentConstraintSystem = components.session.inferenceComponents.createConstraintSystem()
    val currentConstraintStorage: ConstraintStorage get() = currentConstraintSystem.currentStorage()

    private val unitType: ConeClassLikeType = components.session.builtinTypes.unitType.type

    override fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement {
        // TODO
    }

    override fun <T> addPartiallyResolvedCall(call: T) where T : FirResolvable, T : FirStatement {
        // TODO
    }

    override fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement = true

    override fun <R> onCandidatesResolution(call: FirFunctionCall, candidatesResolutionCallback: () -> R): R {
        return if (!call.isAnyOfDelegateOperators())
            candidatesResolutionCallback()
        else
            resolutionContext.bodyResolveContext.withOuterConstraintStorage(
                currentConstraintSystem.currentStorage(),
                candidatesResolutionCallback
            )
    }

    override fun <T> skipCompletion(
        call: T,
        resolutionMode: ResolutionMode,
        completionMode: ConstraintSystemCompletionMode
    ): Boolean where T : FirResolvable, T : FirStatement {
        if (!call.candidate.isSuccessful && call.isOperatorCallWithName { it == OperatorNameConventions.PROVIDE_DELEGATE }) return false
        // Do not run completion for provideDelegate/getValue/setValue because they might affect each other
        if (completionMode == ConstraintSystemCompletionMode.FULL && resolutionMode == ResolutionMode.ContextDependentDelegate) return false

        if (resolutionMode == ResolutionMode.ContextDependentDelegate || call.isAnyOfDelegateOperators()) {
            partiallyResolvedCalls.add(call to call.candidate)
            currentConstraintSystem = call.candidate.system
            return true
        }

        return false
    }

    private fun <T> T.isAnyOfDelegateOperators(): Boolean where T : FirResolvable, T : FirStatement = isOperatorCallWithName {
        it == OperatorNameConventions.PROVIDE_DELEGATE || it == OperatorNameConventions.GET_VALUE || it == OperatorNameConventions.SET_VALUE
    }

    private fun <T> T.isOperatorCallWithName(predicate: (Name) -> Boolean): Boolean where T : FirResolvable, T : FirStatement {
        if (this !is FirFunctionCall) return false
        val name = calleeReference.name
        if (!predicate(name)) return false

        return origin == FirFunctionCallOrigin.Operator
    }


    override fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        constraintSystemBuilder: ConstraintSystemBuilder,
        completionMode: ConstraintSystemCompletionMode
    ): Map<ConeTypeVariableTypeConstructor, ConeKotlinType>? = null

    override fun createSyntheticStubTypes(system: NewConstraintSystemImpl): Map<TypeConstructorMarker, ConeStubType> {
        TODO("Not yet implemented")
    }

    override fun fixSyntheticTypeVariableWithNotEnoughInformation(
        typeVariable: TypeVariableMarker,
        completionContext: ConstraintSystemCompletionContext
    ) {
        error("")
    }

    fun completeCandidates(): List<FirResolvable> {
        val commonSystem = currentConstraintSystem

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

    fun createFinalSubstitutor(): ConeSubstitutor {
        val typeContext = components.session.typeContext

        val t = currentConstraintSystem.asReadOnlyStorage()
            .buildAbstractResultingSubstitutor(typeContext) as ConeSubstitutor

        // TODO: TT??
        return ChainedSubstitutor(t, t)
    }

    override fun hasSyntheticTypeVariables(): Boolean = false

    override fun isSyntheticTypeVariable(typeVariable: TypeVariableMarker): Boolean = false
}
