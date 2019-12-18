/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.command

import com.intellij.debugger.DebuggerInvocationUtil
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.engine.events.SuspendContextCommandImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeImpl
import com.intellij.debugger.ui.impl.watch.NodeManagerImpl
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CoroutineDescriptorData
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutines.proxy.CoroutinesDebugProbesProxy
import org.jetbrains.kotlin.idea.debugger.coroutines.util.ProjectNotification
import org.jetbrains.kotlin.idea.debugger.coroutines.view.CoroutinesDebuggerTree

@Deprecated("moved to XCoroutineView")
class RefreshCoroutinesTreeCommand(
    val context: DebuggerContextImpl,
    private val debuggerTree: CoroutinesDebuggerTree
) : SuspendContextCommandImpl(context.suspendContext) {
    val notification = ProjectNotification(debuggerTree.project)

    override fun contextAction() {
        val nodeManagerImpl = debuggerTree.nodeFactory
        val root = nodeManagerImpl.defaultNode
        val suspendContext: SuspendContextImpl? = suspendContext
        if (context.debuggerSession is DebuggerSession && suspendContext is SuspendContextImpl && !suspendContext.isResumed) {
            var infoCache = CoroutinesDebugProbesProxy(suspendContext).dumpCoroutines()
            if (infoCache.isOk()) {
                val evaluationContext = evaluationContext(suspendContext)
                for (state in infoCache.cache) {
                    val descriptor = createCoroutineDescriptorNode(nodeManagerImpl, state, evaluationContext)
                    root.add(descriptor)
                }
                setRoot(root)
            } else {
                debuggerTree.showMessage(KotlinBundle.message("debugger.session.tab.coroutine.message.failure"))
                notification.error(KotlinBundle.message("debugger.session.tab.coroutine.message.error"))
            }
        } else
            debuggerTree.showMessage(KotlinBundle.message("debugger.session.tab.coroutine.message.resume"))
    }

    private fun evaluationContext(suspendContext : SuspendContextImpl) =
         EvaluationContextImpl(suspendContext, suspendContext.frameProxy)

    private fun createCoroutineDescriptorNode(
        nodeFactory: NodeManagerImpl,
        coroutineInfoData: CoroutineInfoData,
        evaluationContext: EvaluationContextImpl
    ): DebuggerTreeNodeImpl {
        return nodeFactory.createNode(
            nodeFactory.getDescriptor(
                null,
                CoroutineDescriptorData(coroutineInfoData)
            ),
            evaluationContext
        )
    }

    private fun setRoot(root: DebuggerTreeNodeImpl) {
        DebuggerInvocationUtil.swingInvokeLater(debuggerTree.project) {
            debuggerTree.mutableModel.setRoot(root)
            debuggerTree.treeChanged()
        }
    }

}
