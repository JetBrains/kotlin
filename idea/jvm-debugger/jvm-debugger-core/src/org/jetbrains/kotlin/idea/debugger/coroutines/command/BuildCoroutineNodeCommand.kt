/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.command

import com.intellij.debugger.DebuggerInvocationUtil
import com.intellij.debugger.engine.events.DebuggerContextCommandImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.PrioritizedTask
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeImpl
import com.intellij.debugger.ui.impl.watch.NodeManagerImpl

@Deprecated("moved to XCoroutineView")
open class BuildCoroutineNodeCommand(
    val node: DebuggerTreeNodeImpl,
    debuggerContext: DebuggerContextImpl,
    val myNodeManager: NodeManagerImpl,
    thread: ThreadReferenceProxyImpl? = null
) : DebuggerContextCommandImpl(debuggerContext, thread) {

    protected val myChildren = mutableListOf<DebuggerTreeNodeImpl>()

    override fun getPriority() = PrioritizedTask.Priority.NORMAL

    protected fun updateUI(scrollToVisible: Boolean) {
        DebuggerInvocationUtil.swingInvokeLater(debuggerContext.project) {
            node.removeAllChildren()
            for (debuggerTreeNode in myChildren) {
                node.add(debuggerTreeNode)
            }
            node.childrenChanged(scrollToVisible)
        }
    }
}