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
import com.intellij.openapi.ui.MessageType
import com.intellij.xdebugger.impl.XDebuggerManagerImpl
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CoroutineDescriptorData
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutines.proxy.CoroutinesDebugProbesProxy
import org.jetbrains.kotlin.idea.debugger.coroutines.proxy.ManagerThreadExecutor
import org.jetbrains.kotlin.idea.debugger.coroutines.proxy.createEvaluationContext
import org.jetbrains.kotlin.idea.debugger.coroutines.view.CoroutinesDebuggerTree

class RefreshCoroutinesTreeCommand(
    val context: DebuggerContextImpl,
    private val debuggerTree: CoroutinesDebuggerTree
) : SuspendContextCommandImpl(context.suspendContext) {

    override fun contextAction() {
        val nf = debuggerTree.nodeFactory
        val root = nf.defaultNode
        val sc: SuspendContextImpl? = suspendContext
        if (context.debuggerSession is DebuggerSession && sc is SuspendContextImpl && !sc.isResumed) {
            val infoCache = CoroutinesDebugProbesProxy(sc).dumpCoroutines()
            if (infoCache.isOk()) {
                val evaluationContext = sc.createEvaluationContext()

                for (state in infoCache.cache) {
                    val descriptor = createCoroutineDescriptorNode(nf, state, evaluationContext)
                    root.add(descriptor)
                }
                setRoot(root)
            } else {
                debuggerTree.showMessage(KotlinBundle.message("debugger.session.tab.coroutine.message.failure"))
                XDebuggerManagerImpl.NOTIFICATION_GROUP.createNotification(
                    KotlinBundle.message("debugger.session.tab.coroutine.message.error"),
                    MessageType.ERROR
                )
                    .notify(debuggerTree.project)
            }
        } else {
            debuggerTree.showMessage(KotlinBundle.message("debugger.session.tab.coroutine.message.resume"))
        }
    }

    private fun createCoroutineDescriptorNode(
        nodeFactory: NodeManagerImpl,
        coroutineInfoData: CoroutineInfoData,
        evaluationContext: EvaluationContextImpl
    ) =
        nodeFactory.createNode(
            nodeFactory.getDescriptor(
                null,
                CoroutineDescriptorData(coroutineInfoData)
            ),
            evaluationContext
        )

    private fun setRoot(root: DebuggerTreeNodeImpl) {
        DebuggerInvocationUtil.swingInvokeLater(debuggerTree.project) {
            debuggerTree.mutableModel.setRoot(root)
            debuggerTree.treeChanged()
        }
    }

}
