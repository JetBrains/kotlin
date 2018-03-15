/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.resolve.calls.components.InferenceSession
import org.jetbrains.kotlin.resolve.calls.model.CallResolutionResult

abstract class ManyCandidatesResolver : InferenceSession {
    private val partiallyResolvedCalls = arrayListOf<CallResolutionResult>()

    override fun shouldFixTypeVariables(): Boolean {
        return false
    }

    override fun addPartiallyResolvedCall(call: CallResolutionResult) {
        partiallyResolvedCalls.add(call)
    }
}