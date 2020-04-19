/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.intellij.xdebugger.frame.XNamedValue
import org.jetbrains.kotlin.idea.debugger.coroutine.data.*
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.DebugMetadata
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.DebugProbesImpl
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

    fun mapToCoroutineInfoData(mirror: MirrorOfCoroutineInfo): CoroutineInfoData? {
        val cnis = CoroutineNameIdState.instance(mirror)
        val stackTrace = mirror.enchancedStackTrace?.mapNotNull { it.stackTraceElement() } ?: emptyList()
        val variables: List<XNamedValue> = mirror.lastObservedFrame?.let {
            val spilledVariables = debugMetadata?.baseContinuationImpl?.mirror(it, executionContext)
            spilledVariables?.spilledValues(executionContext)
        } ?: emptyList()
        var stackFrames = findStackFrames(stackTrace, variables)
        return CoroutineInfoData(
            cnis,
            stackFrames.restoredStackFrames,
            stackFrames.creationStackFrames,
            mirror.lastObservedThread,
            mirror.lastObservedFrame
        )
    }

    fun isInstalled(): Boolean {
        try {
            return debugProbesImpl?.isInstalledValue ?: false
        } catch (e: Exception) {
            log.error("Exception happened while checking agent status.", e)
            return false
        }
    }

    private fun findStackFrames(
        frames: List<StackTraceElement>,
        variables: List<XNamedValue>
    ): CoroutineStackFrames {
        val index = frames.indexOfFirst { it.isCreationSeparatorFrame() }
        val restoredStackFrames = frames.take(index).map {
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
            if (agentProxy.isInstalled())
                return agentProxy
            else
                return null
        }
    }

}
