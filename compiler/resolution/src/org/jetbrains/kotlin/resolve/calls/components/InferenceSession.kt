/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType

interface InferenceSession {
    companion object {
        val default = object : InferenceSession {
            override fun shouldRunCompletion(candidate: KotlinResolutionCandidate): Boolean = true
            override fun addPartialCallInfo(callInfo: PartialCallInfo) {}
            override fun addErrorCallInfo(callInfo: ErrorCallInfo) {}
            override fun addCompletedCallInfo(callInfo: CompletedCallInfo) {}
            override fun currentConstraintSystem(): ConstraintStorage = ConstraintStorage.Empty
            override fun inferPostponedVariables(
                lambda: ResolvedLambdaAtom,
                initialStorage: ConstraintStorage,
                diagnosticsHolder: KotlinDiagnosticsHolder
            ): Map<TypeConstructor, UnwrappedType> = emptyMap()

            override fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean = false
            override fun callCompleted(resolvedAtom: ResolvedAtom): Boolean = false
            override fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom) = true
        }
    }

    fun shouldRunCompletion(candidate: KotlinResolutionCandidate): Boolean
    fun addPartialCallInfo(callInfo: PartialCallInfo)
    fun addCompletedCallInfo(callInfo: CompletedCallInfo)
    fun addErrorCallInfo(callInfo: ErrorCallInfo)
    fun currentConstraintSystem(): ConstraintStorage
    fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        initialStorage: ConstraintStorage,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ): Map<TypeConstructor, UnwrappedType>?

    fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean
    fun callCompleted(resolvedAtom: ResolvedAtom): Boolean
    fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom): Boolean
}

interface PartialCallInfo {
    val callResolutionResult: PartialCallResolutionResult
}

interface CompletedCallInfo {
    val callResolutionResult: CompletedCallResolutionResult
}

interface ErrorCallInfo {
    val callResolutionResult: CallResolutionResult
}