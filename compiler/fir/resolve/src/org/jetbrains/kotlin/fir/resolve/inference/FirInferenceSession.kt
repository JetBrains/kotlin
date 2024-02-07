/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl

abstract class FirInferenceSession {
    open fun baseConstraintStorageForCandidate(candidate: Candidate): ConstraintStorage? = null

    open fun customCompletionModeInsteadOfFull(
        call: FirResolvable,
    ): ConstraintSystemCompletionMode? = null

    abstract fun <T> processPartiallyResolvedCall(
        call: T,
        resolutionMode: ResolutionMode,
        completionMode: ConstraintSystemCompletionMode
    ) where T : FirResolvable, T : FirStatement

    open fun runLambdaCompletion(candidate: Candidate, forOverloadByLambdaReturnType: Boolean, block: () -> Unit): ConstraintStorage? {
        block()
        return null
    }

    open fun <T> runCallableReferenceResolution(candidate: Candidate, block: () -> T): T = block()

    open fun addSubtypeConstraintIfCompatible(lowerType: ConeKotlinType, upperType: ConeKotlinType, element: FirElement) {}

    companion object {
        val DEFAULT: FirInferenceSession = object : FirInferenceSession() {
            override fun <T> processPartiallyResolvedCall(
                call: T,
                resolutionMode: ResolutionMode,
                completionMode: ConstraintSystemCompletionMode,
            ) where T : FirResolvable, T : FirStatement {
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
