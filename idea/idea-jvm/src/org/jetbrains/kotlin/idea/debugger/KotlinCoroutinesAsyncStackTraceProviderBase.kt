/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.engine.AsyncStackTraceProvider
import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.memory.utils.StackFrameItem

interface KotlinCoroutinesAsyncStackTraceProviderBase : AsyncStackTraceProvider {
    override fun getAsyncStackTrace(stackFrame: JavaStackFrame, suspendContext: SuspendContextImpl): List<StackFrameItem>?
}