/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.model.CallResolutionResult

interface InferenceSession {
    companion object {
        val default = object : InferenceSession {
            override fun prepareBeforeCompletion(commonSystem: NewConstraintSystem) {}
            override fun shouldFixTypeVariables(): Boolean = false
            override fun addPartiallyResolvedCall(call: CallResolutionResult) {}
        }
    }

    fun prepareBeforeCompletion(commonSystem: NewConstraintSystem)
    fun shouldFixTypeVariables(): Boolean
    fun addPartiallyResolvedCall(call: CallResolutionResult)
}