/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.intellij.debugger.engine.DebuggerManagerThreadImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.kotlin.idea.debugger.coroutine.util.CoroutineFrameBuilder
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineInfoCache
import org.jetbrains.kotlin.idea.debugger.coroutine.util.executionContext
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

class CoroutineDebugProbesProxy(val suspendContext: SuspendContextImpl) {
    private val log by logger

    /**
     * Invokes DebugProbes from debugged process's classpath and returns states of coroutines
     * Should be invoked on debugger manager thread
     */
    @Synchronized
    fun dumpCoroutines(): CoroutineInfoCache {
        DebuggerManagerThreadImpl.assertIsManagerThread()
        val coroutineInfoCache = CoroutineInfoCache()
        try {
            val executionContext = suspendContext.executionContext() ?: return coroutineInfoCache.fail()
            val libraryAgentProxy = findProvider(executionContext) ?: return coroutineInfoCache.ok()
            val infoList = libraryAgentProxy.dumpCoroutinesInfo()
            coroutineInfoCache.ok(infoList)
        } catch (e: Throwable) {
            log.error("Exception is thrown by calling dumpCoroutines.", e)
            coroutineInfoCache.fail()
        }
        return coroutineInfoCache
    }

    private fun findProvider(executionContext: DefaultExecutionContext): CoroutineInfoProvider? {
        val agentProxy = CoroutineLibraryAgent2Proxy.instance(executionContext)
        if (agentProxy != null)
            return agentProxy
        if (standaloneCoroutineDebuggerEnabled())
            return CoroutineNoLibraryProxy(executionContext)
        return null
    }
}

fun standaloneCoroutineDebuggerEnabled() = Registry.`is`("kotlin.debugger.coroutines.standalone")
