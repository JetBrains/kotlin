/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.view

import com.intellij.debugger.DebuggerBundle
import com.intellij.debugger.engine.events.DebuggerCommandImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.ui.impl.ThreadsDebuggerTree
import com.intellij.debugger.ui.impl.watch.*
import com.intellij.debugger.ui.tree.StackFrameDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.debugger.coroutines.command.CoroutineBuildCreationFrameCommand
import org.jetbrains.kotlin.idea.debugger.coroutines.command.CoroutineBuildFrameCommand
import org.jetbrains.kotlin.idea.debugger.coroutines.command.RefreshCoroutinesTreeCommand
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CoroutineDescriptorImpl
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CreationFramesDescriptor
import org.jetbrains.kotlin.idea.debugger.coroutines.proxy.ManagerThreadExecutor
import org.jetbrains.kotlin.idea.debugger.coroutines.util.logger
import java.util.*

/**
 * Tree of coroutines for [CoroutinesPanel]
 */
class CoroutinesDebuggerTree(project: Project) : ThreadsDebuggerTree(project) {
    private val log by logger

    // called on every step/frame
    override fun build(context: DebuggerContextImpl) {
        val session = context.debuggerSession

        val command = RefreshCoroutinesTreeCommand(context, this)
        val debuggerSessionState = session?.state ?: DebuggerSession.State.DISPOSED

        if (ApplicationManager.getApplication().isUnitTestMode || debuggerSessionState in EnumSet.of(DebuggerSession.State.PAUSED, DebuggerSession.State.RUNNING)) {
            showMessage(MessageDescriptor.EVALUATING)
            ManagerThreadExecutor(context.debugProcess!!).schedule(command)
        } else {
            showMessage(session?.stateDescription ?: DebuggerBundle.message("status.debug.stopped"))
        }
    }


    override fun getBuildNodeCommand(node: DebuggerTreeNodeImpl): DebuggerCommandImpl? {
        return when(val descriptor = node.descriptor) {
            is CoroutineDescriptorImpl ->
                CoroutineBuildFrameCommand(node, descriptor, myNodeManager, debuggerContext)
            is CreationFramesDescriptor ->
                CoroutineBuildCreationFrameCommand(node, descriptor, myNodeManager, debuggerContext)
            else -> null
        }
    }

    override fun createNodeManager(project: Project): NodeManagerImpl {
        return object : NodeManagerImpl(project, this) {
            override fun getContextKey(frame: StackFrameProxyImpl?): String? {
                return "CoroutinesView"
            }
        }
    }

    override fun isExpandable(node: DebuggerTreeNodeImpl): Boolean {
        val descriptor = node.descriptor
        return if (descriptor is StackFrameDescriptor) false else descriptor.isExpandable
    }
}

class CoroutineInfoCache(val cache: MutableList<CoroutineInfoData> = mutableListOf(), var state: CacheState = CacheState.INIT
) {
    fun ok(infoList: List<CoroutineInfoData>) {
        cache.clear()
        cache.addAll(infoList)
        state = CacheState.OK
    }

    fun fail() {
        cache.clear()
        state = CacheState.FAIL
    }

    fun isOk() : Boolean {
        return state == CacheState.OK
    }
}

enum class CacheState() {
    OK,FAIL,INIT
}
