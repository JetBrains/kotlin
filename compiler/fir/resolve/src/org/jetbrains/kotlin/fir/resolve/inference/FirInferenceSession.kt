/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl

abstract class FirInferenceSession {
    open fun baseConstraintStorageForCandidate(candidate: Candidate, bodyResolveContext: BodyResolveContext): ConstraintStorage? = null

    open fun customCompletionModeInsteadOfFull(
        call: FirResolvable,
    ): ConstraintSystemCompletionMode? = null

    abstract fun <T> processPartiallyResolvedCall(
        call: T,
        resolutionMode: ResolutionMode,
        completionMode: ConstraintSystemCompletionMode
    ) where T : FirResolvable, T : FirExpression

    open fun runLambdaCompletion(candidate: Candidate, forOverloadByLambdaReturnType: Boolean, block: () -> Unit): ConstraintStorage? {
        block()
        return null
    }

    open fun <T> runCallableReferenceResolution(candidate: Candidate, block: () -> T): T = block()

    open fun addSubtypeConstraintIfCompatible(lowerType: ConeKotlinType, upperType: ConeKotlinType, element: FirElement) {}

    /**
     * For non-trivial inference session (currently PCLA-only), if the type is a type variable that might be fixed,
     * fix it and return a fixation result.
     *
     * Type variable might be fixed if it doesn't belong to an outer CS and have proper constraints.
     *
     * By semi-fixation we mean that only the relevant EQUALITY constraint is added,
     * [org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionContext.fixVariable] is not expected to be called.
     *
     * See `getAndSemiFixCurrentResultIfTypeVariable` chapter at [docs/fir/pcla.md]
     *
     * NB: The callee must pay attention that exactly current common CS will be modified.
     */
    open fun getAndSemiFixCurrentResultIfTypeVariable(type: ConeKotlinType): ConeKotlinType? = null

    companion object {
        val DEFAULT: FirInferenceSession = object : FirInferenceSession() {
            override fun <T> processPartiallyResolvedCall(
                call: T,
                resolutionMode: ResolutionMode,
                completionMode: ConstraintSystemCompletionMode,
            ) where T : FirResolvable, T : FirExpression {
                // Do nothing
            }
        }

        @JvmStatic
        protected fun prepareSharedBaseSystem(
            outerSystem: NewConstraintSystemImpl,
            components: InferenceComponents,
        ): NewConstraintSystemImpl {
            return components.createConstraintSystem().apply {
                addOuterSystem(outerSystem.currentStorage())
            }
        }
    }
}
