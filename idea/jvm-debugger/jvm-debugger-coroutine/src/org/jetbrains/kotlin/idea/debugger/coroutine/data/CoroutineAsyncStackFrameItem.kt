/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.data

import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.debugger.ui.impl.watch.MethodsTracker
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl
import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XStackFrame
import com.sun.jdi.Location
import com.sun.jdi.ObjectReference
import org.jetbrains.kotlin.idea.debugger.*
import org.jetbrains.kotlin.idea.debugger.coroutine.util.EmptyStackFrameDescriptor
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger

class CreationCoroutineStackFrameItem(
    val frame: StackFrameProxyImpl,
    val stackTraceElement: StackTraceElement,
    location: Location
) : CoroutineStackFrameItem(location, emptyList()) {
    fun emptyDescriptor() =
        EmptyStackFrameDescriptor(stackTraceElement, frame)
}

class SuspendCoroutineStackFrameItem(
    val frame: StackFrameProxyImpl,
    val stackTraceElement: StackTraceElement,
    val lastObservedFrameFieldRef: ObjectReference,
    location: Location,
    spilledVariables: List<XNamedValue> = emptyList()
) : CoroutineStackFrameItem(location, spilledVariables) {
    fun emptyDescriptor() =
        EmptyStackFrameDescriptor(stackTraceElement, frame)
}

class RunningCoroutineStackFrameItem(
    val frame: StackFrameProxyImpl,
    val stackFrame: XStackFrame,
    spilledVariables: List<XNamedValue> = emptyList()
) : CoroutineStackFrameItem(frame.location(), spilledVariables)

class RestoredCoroutineStackFrameItem(
    val frame: StackFrameProxyImpl,
    location: Location,
    spilledVariables: List<XNamedValue>
) : CoroutineStackFrameItem(location, spilledVariables) {
    fun emptyDescriptor() =
        StackFrameDescriptorImpl(frame, MethodsTracker())
}

class DefaultCoroutineStackFrameItem(location: Location, spilledVariables: List<XNamedValue>) :
    CoroutineStackFrameItem(location, spilledVariables)

sealed class CoroutineStackFrameItem(val location: Location, val spilledVariables: List<XNamedValue>) :
    StackFrameItem(location, spilledVariables) {
    val log by logger

    fun uniqueId(): String {
        return location.safeSourceName() + ":" + location.safeMethod().toString() + ":" +
                location.safeLineNumber() + ":" + location.safeKotlinPreferredLineNumber()
    }
}