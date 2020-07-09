/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.intellij.xdebugger.frame.XNamedValue
import com.sun.jdi.ObjectReference
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineNameIdState
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CreationCoroutineStackFrameItem
import org.jetbrains.kotlin.idea.debugger.coroutine.data.SuspendCoroutineStackFrameItem
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.DebugMetadata
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.DebugProbesImpl
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.MirrorOfBaseContinuationImpl
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.MirrorOfCoroutineInfo
import org.jetbrains.kotlin.idea.debugger.coroutine.util.isCreationSeparatorFrame
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

class CoroutineLibraryAgent2Proxy(private val executionContext: DefaultExecutionContext) :
    CoroutineInfoProvider {
    val log by logger
    private val debugProbesImpl = DebugProbesImpl.instance(executionContext)
    private val locationCache = LocationCache(executionContext)
    private val debugMetadata: DebugMetadata? = DebugMetadata.instance(executionContext)

    override fun dumpCoroutinesInfo(): List<CoroutineInfoData> {
        val result = debugProbesImpl?.dumpCoroutinesInfo(executionContext) ?: emptyList()
        return result.mapNotNull { mapToCoroutineInfoData(it) }
    }

    private fun mapToCoroutineInfoData(mirror: MirrorOfCoroutineInfo): CoroutineInfoData? {
        val coroutineNameIdState = CoroutineNameIdState.instance(mirror)
        val stackTrace = mirror.enhancedStackTrace?.mapNotNull { it.stackTraceElement() } ?: emptyList()
        val baseContinuationImpls = getAllBaseContinuationImpls(mirror.lastObservedFrame)
        val stackFrames = findStackFrames(stackTrace, baseContinuationImpls)
        return CoroutineInfoData(
            coroutineNameIdState,
            stackFrames.restoredStackFrames,
            stackFrames.creationStackFrames,
            mirror.lastObservedThread,
            mirror.lastObservedFrame
        )
    }

    /**
     * Restores array of BaseContinuationImpl's for each restored frame based on the CoroutineInfo's last frame.
     * Start from 'lastObservedFrame' and following 'completion' property until the end of the chain (completion = null).
     */
    private fun getAllBaseContinuationImpls(lastObservedFrame: ObjectReference?): List<MirrorOfBaseContinuationImpl> {
        val restoredBaseContinuationImpl = mutableListOf<MirrorOfBaseContinuationImpl>()
        var observedFrame = lastObservedFrame
        while (observedFrame != null) {
            val baseContinuationImpl = debugMetadata?.baseContinuationImpl?.mirror(observedFrame, executionContext)
            if (baseContinuationImpl != null) {
                restoredBaseContinuationImpl.add(baseContinuationImpl)
                observedFrame = baseContinuationImpl.nextContinuation
            } else
                break
        }
        return restoredBaseContinuationImpl
    }

    fun isInstalled(): Boolean {
        return try {
            debugProbesImpl?.isInstalledValue ?: false
        } catch (e: Exception) {
            log.error("Exception happened while checking agent status.", e)
            false
        }
    }

    private fun findStackFrames(
        frames: List<StackTraceElement>,
        baseContinuationList: List<MirrorOfBaseContinuationImpl>
    ): CoroutineStackFrames {
        val index = frames.indexOfFirst { it.isCreationSeparatorFrame() }
        val restoredStackTraceElements = if (index >= 0)
            frames.take(index)
        else
            frames

        val restoredStackFrames = restoredStackTraceElements.mapIndexed { index, it ->
            val variables = baseContinuationList.getOrNull(index)?.spilledValues(executionContext) ?: emptyList()
            SuspendCoroutineStackFrameItem(it, locationCache.createLocation(it), variables)
        }
        val creationStackFrames = frames.subList(index + 1, frames.size).mapIndexed { ix, it ->
            CreationCoroutineStackFrameItem(it, locationCache.createLocation(it), ix == 0)
        }
        return CoroutineStackFrames(restoredStackFrames, creationStackFrames)
    }

    data class CoroutineStackFrames(
        val restoredStackFrames: List<SuspendCoroutineStackFrameItem>,
        val creationStackFrames: List<CreationCoroutineStackFrameItem>
    )

    companion object {
        fun instance(executionContext: DefaultExecutionContext): CoroutineLibraryAgent2Proxy? {
            val agentProxy = CoroutineLibraryAgent2Proxy(executionContext)
            return if (agentProxy.isInstalled())
                agentProxy
            else
                null
        }
    }

}
