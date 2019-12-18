/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.command

import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeImpl
import com.intellij.debugger.ui.impl.watch.NodeManagerImpl
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CreationFramesDescriptor
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CoroutineCreatedStackFrameDescriptor

@Deprecated("moved to XCoroutineView")
class CoroutineBuildCreationFrameCommand(
    node: DebuggerTreeNodeImpl,
    val descriptor: CreationFramesDescriptor,
    nodeManager: NodeManagerImpl,
    debuggerContext: DebuggerContextImpl
) : BuildCoroutineNodeCommand(node, debuggerContext, nodeManager) {
    override fun threadAction() {
        val threadProxy = debuggerContext.suspendContext?.thread ?: return
        val evalContext = debuggerContext.createEvaluationContext() ?: return
        val proxy = threadProxy.forceFrames().first()
        for(it in descriptor.frames) {
            val descriptor = myNodeManager.createNode(
                CoroutineCreatedStackFrameDescriptor(it, proxy), evalContext)
            myChildren.add(descriptor)

        }
        updateUI(true)
    }
}