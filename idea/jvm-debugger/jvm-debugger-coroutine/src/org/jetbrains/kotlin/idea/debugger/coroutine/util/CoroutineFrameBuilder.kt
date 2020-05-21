/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.util

import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.xdebugger.frame.XNamedValue
import com.sun.jdi.ObjectReference
import org.jetbrains.kotlin.idea.debugger.coroutine.data.*
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.ContinuationHolder
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.SkipCoroutineStackFrameProxyImpl
import java.lang.Integer.min


class CoroutineFrameBuilder {

    companion object {
        val log by logger
        private const val PRE_FETCH_FRAME_COUNT = 5

        fun build(coroutine: CoroutineInfoData, suspendContext: SuspendContextImpl): CoroutineFrameItemLists? =
            when {
                coroutine.isRunning() -> buildStackFrameForActive(coroutine, suspendContext)
                coroutine.isSuspended() -> CoroutineFrameItemLists(coroutine.stackTrace, coroutine.creationStackTrace)
                else -> null
            }

        private fun buildStackFrameForActive(coroutine: CoroutineInfoData, suspendContext: SuspendContextImpl): CoroutineFrameItemLists? {
            val activeThread = coroutine.activeThread ?: return null

            val coroutineStackFrameList = mutableListOf<CoroutineStackFrameItem>()
            val threadReferenceProxyImpl = ThreadReferenceProxyImpl(suspendContext.debugProcess.virtualMachineProxy, activeThread)
            val realFrames = threadReferenceProxyImpl.forceFrames()
            for (runningStackFrameProxy in realFrames) {
                val preflightStackFrame = coroutineExitFrame(runningStackFrameProxy, suspendContext)
                if (preflightStackFrame != null) {
                    buildRealStackFrameItem(preflightStackFrame.stackFrameProxy)?.let {
                        coroutineStackFrameList.add(it)
                    }

                    val coroutineFrameLists = build(preflightStackFrame, suspendContext)
                    coroutineStackFrameList.addAll(coroutineFrameLists.frames)
                    return CoroutineFrameItemLists(coroutineStackFrameList, coroutineFrameLists.creationFrames)
                } else {
                    buildRealStackFrameItem(runningStackFrameProxy)?.let {
                        coroutineStackFrameList.add(it)
                    }
                }
            }
            return CoroutineFrameItemLists(coroutineStackFrameList, emptyList())
        }

        /**
         * Used by CoroutineAsyncStackTraceProvider to build XFramesView
         */
        fun build(preflightFrame: CoroutinePreflightFrame, suspendContext: SuspendContextImpl): CoroutineFrameItemLists {
            val stackFrames = mutableListOf<CoroutineStackFrameItem>()

            val (restoredStackTrace, variablesRemovedFromBottomRestoredFrame) = restoredStackTrace(
                preflightFrame
            )
            stackFrames.addAll(restoredStackTrace)

            // @TODO perhaps we need to merge the dropped variables with the frame below...
            val framesLeft = preflightFrame.threadPreCoroutineFrames
            stackFrames.addAll(framesLeft.mapIndexedNotNull { index, stackFrameProxyImpl ->
                suspendContext.invokeInManagerThread { buildRealStackFrameItem(stackFrameProxyImpl) }
            })

            return CoroutineFrameItemLists(stackFrames, preflightFrame.coroutineInfoData.creationStackTrace)
        }

        fun restoredStackTrace(preflightFrame: CoroutinePreflightFrame): Pair<List<CoroutineStackFrameItem>, List<XNamedValue>> {
            val preflightFrameLocation = preflightFrame.stackFrameProxy.location()
            val coroutineStackFrame = preflightFrame.coroutineInfoData.stackTrace
            val preCoroutineTopFrameLocation = preflightFrame.threadPreCoroutineFrames.firstOrNull()?.location()

            val variablesRemovedFromTopRestoredFrame = mutableListOf<XNamedValue>()
            val stripTopStackTrace = coroutineStackFrame.dropWhile {
                it.location.isFilterFromTop(preflightFrameLocation).apply {
                    if (this)
                        variablesRemovedFromTopRestoredFrame.addAll(it.spilledVariables)
                }
            }
            // @TODO Need to merge variablesRemovedFromTopRestoredFrame into stripTopStackTrace.firstOrNull().spilledVariables
            val variablesRemovedFromBottomRestoredFrame = mutableListOf<XNamedValue>()
            val restoredFrames = when (preCoroutineTopFrameLocation) {
                null -> stripTopStackTrace
                else ->
                    stripTopStackTrace.dropLastWhile {
                        it.location.isFilterFromBottom(preCoroutineTopFrameLocation)
                            .apply { variablesRemovedFromBottomRestoredFrame.addAll(it.spilledVariables) }
                    }
            }
            return Pair(restoredFrames, variablesRemovedFromBottomRestoredFrame)
        }

        data class CoroutineFrameItemLists(
            val frames: List<CoroutineStackFrameItem>,
            val creationFrames: List<CreationCoroutineStackFrameItem>
        ) {
            fun allFrames() =
                frames + creationFrames
        }

        private fun buildRealStackFrameItem(
            frame: StackFrameProxyImpl
        ): RunningCoroutineStackFrameItem? {
            val location = frame.location() ?: return null
            return if (!location.safeCoroutineExitPointLineNumber())
                RunningCoroutineStackFrameItem(SkipCoroutineStackFrameProxyImpl(frame))
            else
                null
        }

        /**
         * Used by CoroutineStackFrameInterceptor to check if that frame is 'exit' coroutine frame.
         */
        fun coroutineExitFrame(
            frame: StackFrameProxyImpl,
            suspendContext: SuspendContextImpl
        ): CoroutinePreflightFrame? {
            return suspendContext.invokeInManagerThread {
                val sem = frame.location().isPreFlight()
                val preflightStackFrame = if (sem.isCoroutineFound()) {
                    lookupContinuation(suspendContext, frame, sem)
                } else
                    null
                preflightStackFrame
            }
        }

        fun lookupContinuation(
            suspendContext: SuspendContextImpl,
            frame: StackFrameProxyImpl,
            mode: SuspendExitMode
        ): CoroutinePreflightFrame? {
            if (!mode.isCoroutineFound())
                return null

            val theFollowingFrames = theFollowingFrames(frame) ?: emptyList()
            val suspendParameterFrame = if (mode.isSuspendMethodParameter()) {
                if (theFollowingFrames.isNotEmpty()) {
                    // have to check next frame if that's invokeSuspend:-1 before proceed, otherwise skip
                    lookForTheFollowingFrame(theFollowingFrames) ?: return null
                } else
                    return null
            } else
                null

            if (threadAndContextSupportsEvaluation(suspendContext, frame)) {
                val context = suspendContext.executionContext() ?: return null
                val continuation = when (mode) {
                    SuspendExitMode.SUSPEND_LAMBDA -> getThisContinuation(frame)
                    SuspendExitMode.SUSPEND_METHOD_PARAMETER -> getLVTContinuation(frame)
                    else -> null
                } ?: return null

                val continuationHolder = ContinuationHolder.instance(context)
                val coroutineInfo = continuationHolder.extractCoroutineInfoData(continuation) ?: return null
                return CoroutinePreflightFrame(
                    coroutineInfo,
                    frame,
                    theFollowingFrames,
                    mode
                )
            }
            return null
        }

        private fun lookForTheFollowingFrame(theFollowingFrames: List<StackFrameProxyImpl>): StackFrameProxyImpl? {
            for (i in 0 until min(PRE_FETCH_FRAME_COUNT, theFollowingFrames.size)) { // pre-scan PRE_FETCH_FRAME_COUNT frames
                val nextFrame = theFollowingFrames[i]
                if (nextFrame.location().isPreFlight() == SuspendExitMode.SUSPEND_METHOD) {
                    return nextFrame
                }
            }
            return null
        }

        private fun getLVTContinuation(frame: StackFrameProxyImpl?) =
            frame?.continuationVariableValue()

        private fun getThisContinuation(frame: StackFrameProxyImpl?): ObjectReference? =
            frame?.thisVariableValue()

        private fun theFollowingFrames(frame: StackFrameProxyImpl): List<StackFrameProxyImpl>? {
            val frames = frame.threadProxy().frames()
            val indexOfCurrentFrame = frames.indexOf(frame)
            if (indexOfCurrentFrame >= 0) {
                val indexOfGetCoroutineSuspended = hasGetCoroutineSuspended(frames)
                // @TODO if found - skip this thread stack
                if (indexOfGetCoroutineSuspended < 0 && frames.size > indexOfCurrentFrame + 1)
                    return frames.drop(indexOfCurrentFrame + 1)
            } else {
                log.error("Frame isn't found on the thread stack.")
            }
            return null
        }
    }
}
