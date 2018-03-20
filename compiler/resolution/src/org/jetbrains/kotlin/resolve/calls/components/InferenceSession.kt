/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.model.CallResolutionResult
import org.jetbrains.kotlin.resolve.calls.model.KotlinResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.model.PartialCallResolutionResult

interface InferenceSession {
    companion object {
        val default = object : InferenceSession {
            override fun shouldRunCompletion(candidate: KotlinResolutionCandidate): Boolean = true
            override fun addPartialCallInfo(callInfo: PartialCallInfo) {}
            override fun addErrorCallInfo(callInfo: ErrorCallInfo) {}
            override fun currentConstraintSystem(): ConstraintStorage = ConstraintStorage.Empty
        }
    }

    fun shouldRunCompletion(candidate: KotlinResolutionCandidate): Boolean
    fun addPartialCallInfo(callInfo: PartialCallInfo)
    fun addErrorCallInfo(callInfo: ErrorCallInfo)
    fun currentConstraintSystem(): ConstraintStorage
}

interface PartialCallInfo {
    val callResolutionResult: PartialCallResolutionResult
}

interface ErrorCallInfo {
    val callResolutionResult: CallResolutionResult
}