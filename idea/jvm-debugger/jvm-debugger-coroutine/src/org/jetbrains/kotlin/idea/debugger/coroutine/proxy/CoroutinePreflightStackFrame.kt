/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.DebuggerManagerThreadImpl
import com.intellij.debugger.engine.JVMStackFrameInfoProvider
import com.intellij.debugger.jdi.GeneratedLocation
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.debugger.ui.impl.watch.MethodsTracker
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XValueChildrenList
import com.sun.jdi.Location
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineStackFrameItem
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CreationCoroutineStackFrame
import org.jetbrains.kotlin.idea.debugger.invokeInManagerThread
import org.jetbrains.kotlin.idea.debugger.safeLineNumber
import org.jetbrains.kotlin.idea.debugger.safeLocation
import org.jetbrains.kotlin.idea.debugger.safeMethod
import org.jetbrains.kotlin.idea.debugger.stackFrame.KotlinStackFrame

/**
 * Coroutine exit frame represented by a stack frames
 * invokeSuspend():-1
 * resumeWith()
 *
 */

class CoroutinePreflightStackFrame(
    val coroutineInfoData: CoroutineInfoData,
    val stackFrameDescriptorImpl: StackFrameDescriptorImpl,
    val threadPreCoroutineFrames: List<StackFrameProxyImpl>,
) : KotlinStackFrame(stackFrameDescriptorImpl), JVMStackFrameInfoProvider {

    override fun computeChildren(node: XCompositeNode) {
        val childrenList = XValueChildrenList()
        val firstRestoredCoroutineStackFrameItem = coroutineInfoData.stackTrace.firstOrNull() ?: return
        firstRestoredCoroutineStackFrameItem.spilledVariables.forEach {
            childrenList.add(it)
        }
        node.addChildren(childrenList, false)
        super.computeChildren(node)
    }

    override fun isInLibraryContent() =
        false

    override fun isSynthetic() =
        false

    companion object {
        fun preflight(
            invokeSuspendFrame: StackFrameProxyImpl,
            coroutineInfoData: CoroutineInfoData,
            originalFrames: List<StackFrameProxyImpl>
        ): CoroutinePreflightStackFrame? {
            val descriptor = createFirstRestoredFrame(invokeSuspendFrame, coroutineInfoData)
            return CoroutinePreflightStackFrame(
                coroutineInfoData,
                descriptor,
                originalFrames.filter { ! isInvokeSuspendNegativeLineMethodFrame(it) }
            )
        }

        fun createFirstRestoredFrame(
            invokeSuspendFrame: StackFrameProxyImpl,
            coroutineInfoData: CoroutineInfoData
        ): StackFrameDescriptorImpl {
            if (coroutineInfoData.stackTrace.size >= 2) {
                // assume firstFrame is invokeSuspend and second is resumeWith
                val fisrtRestoredFrame = coroutineInfoData.stackTrace.removeAt(0)
                val secondRestoredFrame = coroutineInfoData.stackTrace.removeAt(0)
                println(formatLocation(invokeSuspendFrame.location()))
                println(formatLocation(fisrtRestoredFrame.location))
                println(formatLocation(secondRestoredFrame.location))
                val descriptor = StackFrameDescriptorImpl(
                    LocationStackFrameProxyImpl(secondRestoredFrame.location, invokeSuspendFrame), MethodsTracker()
                )
                return descriptor
            } else {
                return StackFrameDescriptorImpl(invokeSuspendFrame, MethodsTracker())
            }
        }

        private fun formatLocation(location: Location): String {
            return "${location.method().name()}:${location.lineNumber()}, ${location.method().declaringType()}"
        }

        private fun isInvokeSuspendNegativeLineMethodFrame(frame: StackFrameProxyImpl) =
            frame.safeLocation()?.safeMethod()?.name() == "invokeSuspend" &&
                    frame.safeLocation()?.safeMethod()?.signature() == "(Ljava/lang/Object;)Ljava/lang/Object;" &&
                    frame.safeLocation()?.safeLineNumber() ?: 0 < 0
    }

}