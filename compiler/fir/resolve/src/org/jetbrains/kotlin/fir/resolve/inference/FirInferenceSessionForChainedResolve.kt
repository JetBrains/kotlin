/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeBuilderInferenceSubstitutionConstraintPosition
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.InitialConstraint
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.types.model.*

abstract class FirInferenceSessionForChainedResolve(
    protected val resolutionContext: ResolutionContext
) : FirInferenceSession() {
    override fun fixSyntheticTypeVariableWithNotEnoughInformation(
        typeVariable: TypeVariableMarker,
        completionContext: ConstraintSystemCompletionContext
    ) {
    }

    protected val partiallyResolvedCalls: MutableList<Pair<FirResolvable, Candidate>> = mutableListOf()
    private val completedCalls: MutableSet<FirResolvable> = mutableSetOf()

    protected val components: BodyResolveComponents
        get() = resolutionContext.bodyResolveComponents

    override fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement {
        // do nothing
    }

    override fun <T> addPartiallyResolvedCall(call: T) where T : FirResolvable, T : FirStatement {
        partiallyResolvedCalls += call to call.candidate
    }

    override fun registerStubTypes(map: Map<TypeVariableMarker, StubTypeMarker>) {}

    protected val FirResolvable.candidate: Candidate
        get() = candidate()!!

    override fun clear() {
        partiallyResolvedCalls.clear()
        completedCalls.clear()
    }

    protected fun integrateConstraintToSystem(
        commonSystem: NewConstraintSystemImpl,
        initialConstraint: InitialConstraint,
        callSubstitutor: ConeSubstitutor,
        nonFixedToVariablesSubstitutor: ConeSubstitutor,
        fixedTypeVariables: Map<TypeConstructorMarker, KotlinTypeMarker>,
    ): Boolean {
        val substitutedConstraintWith =
            initialConstraint.substitute(callSubstitutor).substitute(nonFixedToVariablesSubstitutor, fixedTypeVariables)
        val lower = substitutedConstraintWith.a // TODO: SUB
        val upper = substitutedConstraintWith.b // TODO: SUB

        if (commonSystem.isProperType(lower) && (lower == upper || commonSystem.isProperType(upper))) return false

        val position = substitutedConstraintWith.position
        when (initialConstraint.constraintKind) {
            ConstraintKind.LOWER -> error("LOWER constraint shouldn't be used, please use UPPER")

            ConstraintKind.UPPER -> commonSystem.addSubtypeConstraint(lower, upper, position)

            ConstraintKind.EQUALITY ->
                with(commonSystem) {
                    addSubtypeConstraint(lower, upper, position)
                    addSubtypeConstraint(upper, lower, position)
                }
        }
        return true
    }

    protected fun InitialConstraint.substitute(substitutor: TypeSubstitutorMarker): InitialConstraint {
        val lowerSubstituted = substitutor.safeSubstitute(resolutionContext.typeContext, this.a)
        val upperSubstituted = substitutor.safeSubstitute(resolutionContext.typeContext, this.b)

        if (lowerSubstituted == a && upperSubstituted == b) return this

        return InitialConstraint(
            lowerSubstituted,
            upperSubstituted,
            this.constraintKind,
            ConeBuilderInferenceSubstitutionConstraintPosition(this)
        )
    }

    protected fun InitialConstraint.substitute(
        substitutor: TypeSubstitutorMarker,
        fixedTypeVariables: Map<TypeConstructorMarker, KotlinTypeMarker>
    ): InitialConstraint {
        val substituted = substitute(substitutor)
        val a = a
        // In situation when some type variable _T is fixed to Stub(_T)?,
        // we are not allowed just to substitute Stub(_T) with T because nullabilities are different here!
        // To compensate this, we have to substitute Stub(_T) <: SomeType constraint with T <: SomeType? adding nullability to upper type
        if (a is ConeStubTypeForChainInference && substituted.a !is ConeStubTypeForChainInference) {
            val constructor = a.constructor
            val fixedTypeVariableType = fixedTypeVariables[constructor.variable.typeConstructor]
            if (fixedTypeVariableType is ConeStubTypeForChainInference &&
                fixedTypeVariableType.constructor === constructor &&
                fixedTypeVariableType.isMarkedNullable
            ) {
                return InitialConstraint(
                    substituted.a,
                    (substituted.b as ConeKotlinType).withNullability(ConeNullability.NULLABLE, resolutionContext.typeContext),
                    substituted.constraintKind,
                    substituted.position
                )
            }
        }
        return substituted
    }
}
