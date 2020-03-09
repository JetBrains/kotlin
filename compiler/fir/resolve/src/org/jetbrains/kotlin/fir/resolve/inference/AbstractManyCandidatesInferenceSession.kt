/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

abstract class AbstractManyCandidatesInferenceSession(
    protected val components: BodyResolveComponents,
    initialCall: FirExpression,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
) : FirInferenceSession() {
    private val errorCalls: MutableList<FirResolvable> = mutableListOf()
    private val partiallyResolvedCalls: MutableList<FirResolvable> = mutableListOf()
    private val completedCalls: MutableSet<FirResolvable> = mutableSetOf()

    init {
        val initialCandidate = (initialCall as? FirResolvable)
            ?.calleeReference
            ?.safeAs<FirNamedReferenceWithCandidate>()
            ?.candidate
        if (initialCandidate != null) {
            partiallyResolvedCalls += initialCall as FirResolvable
        }
    }

    private val unitType: ConeKotlinType = components.session.builtinTypes.unitType.coneTypeUnsafe()

    override val currentConstraintSystem: ConstraintStorage
        get() = partiallyResolvedCalls.lastOrNull()
            ?.calleeReference
            ?.safeAs<FirNamedReferenceWithCandidate>()
            ?.candidate
            ?.system
            ?.currentStorage()
            ?: ConstraintStorage.Empty

    private lateinit var resultingConstraintSystem: NewConstraintSystem

    override fun shouldRunCompletion(candidate: Candidate): Boolean {
        return false
    }

    override fun <T> addCompetedCall(call: T) where T : FirResolvable, T : FirStatement {
        // do nothing
    }

    final override fun <T> addPartiallyResolvedCall(call: T) where T : FirResolvable, T : FirStatement {
        partiallyResolvedCalls += call
    }

    final override fun <T> addErrorCall(call: T) where T : FirResolvable, T : FirStatement {
        errorCalls += call
    }

    final override fun <T> callCompleted(call: T): Boolean where T : FirResolvable, T : FirStatement {
        return !completedCalls.add(call)
    }

    protected open fun prepareForCompletion(
        commonSystem: NewConstraintSystem,
        partiallyResolvedCalls: List<FirResolvable>
    ) {
        // do nothing
    }

    fun completeCandidates(): List<FirResolvable> {
        fun runCompletion(constraintSystem: NewConstraintSystem, atoms: List<FirStatement>) {
            components.callCompleter.completer.complete(
                constraintSystem.asConstraintSystemCompleterContext(),
                KotlinConstraintSystemCompleter.ConstraintSystemCompletionMode.FULL,
                atoms,
                unitType
            ) {
                postponedArgumentsAnalyzer.analyze(
                    constraintSystem.asPostponedArgumentsAnalyzerContext(),
                    it,
                    (atoms.first() as FirResolvable).candidate
                )
            }
        }

        @Suppress("UNCHECKED_CAST")
        val resolvedCalls = partiallyResolvedCalls as List<FirResolvable>
        val commonSystem = components.inferenceComponents.createConstraintSystem().apply {
            addOtherSystem(currentConstraintSystem)
        }
        prepareForCompletion(commonSystem, resolvedCalls)
        @Suppress("UNCHECKED_CAST")
        runCompletion(commonSystem, resolvedCalls as List<FirStatement>)
        resultingConstraintSystem = commonSystem
        return resolvedCalls
    }

    protected val FirResolvable.candidate: Candidate
        get() = candidate()!!

    fun createFinalSubstitutor(): ConeSubstitutor {
        return resultingConstraintSystem.asReadOnlyStorage()
            .buildAbstractResultingSubstitutor(components.inferenceComponents.ctx) as ConeSubstitutor
    }
}