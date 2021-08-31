/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeVariableTypeConstructor
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage

abstract class FirInferenceSession {
    companion object {
        val DEFAULT: FirInferenceSession = object : FirStubInferenceSession() {}
    }

    abstract fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement
    abstract val currentConstraintSystem: ConstraintStorage

    abstract fun <T> addPartiallyResolvedCall(call: T) where T : FirResolvable, T : FirStatement
    abstract fun <T> addErrorCall(call: T) where T : FirResolvable, T : FirStatement
    abstract fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement

    abstract fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        initialStorage: ConstraintStorage,
        completionMode: ConstraintSystemCompletionMode,
        // TODO: diagnostic holder
    ): Map<ConeTypeVariableTypeConstructor, ConeKotlinType>?

    abstract fun <T> writeOnlyStubs(call: T): Boolean where T : FirResolvable, T : FirStatement
    abstract fun <T> callCompleted(call: T): Boolean where T : FirResolvable, T : FirStatement
    abstract fun <T> shouldCompleteResolvedSubAtomsOf(call: T): Boolean where T : FirResolvable, T : FirStatement
}

abstract class FirStubInferenceSession : FirInferenceSession() {
    override fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement = true

    override val currentConstraintSystem: ConstraintStorage
        get() = ConstraintStorage.Empty

    override fun <T> addPartiallyResolvedCall(call: T) where T : FirResolvable, T : FirStatement {}
    override fun <T> addErrorCall(call: T) where T : FirResolvable, T : FirStatement {}
    override fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement {}

    override fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        initialStorage: ConstraintStorage,
        completionMode: ConstraintSystemCompletionMode
    ): Map<ConeTypeVariableTypeConstructor, ConeKotlinType>? = null

    override fun <T> writeOnlyStubs(call: T): Boolean where T : FirResolvable, T : FirStatement = false
    override fun <T> callCompleted(call: T): Boolean where T : FirResolvable, T : FirStatement = false
    override fun <T> shouldCompleteResolvedSubAtomsOf(call: T): Boolean where T : FirResolvable, T : FirStatement = true
}
