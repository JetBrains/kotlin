/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.view

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaExecutionStack
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.ide.CommonActionsManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CaptionPanel
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.border.CustomLineBorder
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.SingleAlarm
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.impl.actions.XDebuggerActions
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTreePanel
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTreeRestorer
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTreeState
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueContainerNode
import com.sun.jdi.request.EventRequest
import org.jetbrains.kotlin.idea.debugger.coroutine.CoroutineDebuggerContentInfo
import org.jetbrains.kotlin.idea.debugger.coroutine.CoroutineDebuggerContentInfo.Companion.XCOROUTINE_POPUP_ACTION_GROUP
import org.jetbrains.kotlin.idea.debugger.coroutine.KotlinDebuggerCoroutinesBundle
import org.jetbrains.kotlin.idea.debugger.coroutine.VersionedImplementationProvider
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineStackFrameItem
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CreationCoroutineStackFrameItem
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.CoroutineDebugProbesProxy
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.ManagerThreadExecutor
import org.jetbrains.kotlin.idea.debugger.coroutine.util.CreateContentParams
import org.jetbrains.kotlin.idea.debugger.coroutine.util.CreateContentParamsProvider
import org.jetbrains.kotlin.idea.debugger.coroutine.util.XDebugSessionListenerProvider
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import java.awt.BorderLayout
import javax.swing.JPanel


class XCoroutineView(val project: Project, val session: XDebugSession) :
    Disposable, XDebugSessionListenerProvider, CreateContentParamsProvider {
    val log by logger
    val versionedImplementationProvider = VersionedImplementationProvider()

    val mainPanel = JPanel(BorderLayout())
    val someCombobox = ComboBox<String>()
    val panel = XDebuggerTreePanel(project, session.debugProcess.editorsProvider, this, null, XCOROUTINE_POPUP_ACTION_GROUP, null)
    val alarm = SingleAlarm(Runnable { resetRoot() }, VIEW_CLEAR_DELAY, this)
//    val javaDebugProcess =
//    val debugProcess: DebugProcessImpl = javaDebugProcess.debuggerSession.process
    val renderer = SimpleColoredTextIconPresentationRenderer()
    val managerThreadExecutor = ManagerThreadExecutor(session)
    var treeState: XDebuggerTreeState? = null
    private var restorer: XDebuggerTreeRestorer? = null
    private var selectedNodeListener: XDebuggerTreeSelectedNodeListener? = null

    companion object {
        private val VIEW_CLEAR_DELAY = 100 //ms
    }

    init {
        someCombobox.renderer = versionedImplementationProvider.comboboxListCellRenderer()
        object : ComboboxSpeedSearch(someCombobox) {
            override fun getElementText(element: Any?): String? {
                return element.toString()
            }
        }
        someCombobox.addItem(null)
        val myToolbar = createToolbar()
        val myThreadsPanel = Wrapper()
        myThreadsPanel.border = CustomLineBorder(CaptionPanel.CNT_ACTIVE_BORDER_COLOR, 0, 0, 1, 0)
        myThreadsPanel.add(myToolbar?.component, BorderLayout.EAST)
        myThreadsPanel.add(someCombobox, BorderLayout.CENTER)
        mainPanel.add(panel.mainPanel, BorderLayout.CENTER)
        selectedNodeListener = XDebuggerTreeSelectedNodeListener(session, panel.tree)
        selectedNodeListener?.installOn()
    }

    private fun createToolbar(): ActionToolbarImpl? {
        val framesGroup = DefaultActionGroup()
        val actionsManager = CommonActionsManager.getInstance()
        framesGroup
            .addAll(ActionManager.getInstance().getAction(XDebuggerActions.FRAMES_TOP_TOOLBAR_GROUP))
        val toolbar = ActionManager.getInstance().createActionToolbar(
            ActionPlaces.DEBUGGER_TOOLBAR, framesGroup, true
        ) as ActionToolbarImpl
        toolbar.setReservePlaceAutoPopupIcon(false)
        return toolbar
    }

    fun saveState() {
        DebuggerUIUtil.invokeLater {
            if (!(panel.tree.root is EmptyNode)) {
                treeState = XDebuggerTreeState.saveState(panel.tree)
            }
        }
    }

    fun resetRoot() {
        DebuggerUIUtil.invokeLater {
            panel.tree.setRoot(EmptyNode(), false)
        }
    }

    fun renewRoot(suspendContext: SuspendContextImpl) {
        panel.tree.setRoot(XCoroutinesRootNode(suspendContext), false)
        if (treeState != null) {
            restorer?.dispose()
            restorer = treeState?.restoreState(panel.tree)
        }
    }

    override fun dispose() {
        if (restorer != null) {
            restorer?.dispose()
            restorer = null
        }
    }

    fun forceClear() {
        alarm.cancel()
    }

    override fun debugSessionListener(session: XDebugSession) =
        CoroutineViewDebugSessionListener(session, this)

    override fun createContentParams(): CreateContentParams =
        CreateContentParams(
            CoroutineDebuggerContentInfo.XCOROUTINE_THREADS_CONTENT,
            mainPanel,
            KotlinDebuggerCoroutinesBundle.message("coroutine.view.title"),
            null,
            panel.tree
        )

    inner class EmptyNode : XValueContainerNode<XValueContainer>(panel.tree, null, true, object : XValueContainer() {})

    inner class XCoroutinesRootNode(suspendContext: SuspendContextImpl) :
        XValueContainerNode<CoroutineGroupContainer>(
            panel.tree, null, false,
            CoroutineGroupContainer(suspendContext, KotlinDebuggerCoroutinesBundle.message("coroutine.view.default.group"))
        )

    inner class CoroutineGroupContainer(val suspendContext: SuspendContextImpl, val groupName: String) : XValueContainer() {
        override fun computeChildren(node: XCompositeNode) {
            if (suspendContext.suspendPolicy == EventRequest.SUSPEND_ALL) {
                val groups = XValueChildrenList.singleton(CoroutineContainer(suspendContext, groupName))
                node.addChildren(groups, true)
            } else {
                node.addChildren(
                    XValueChildrenList.singleton(ErrorNode("to.enable.information.breakpoint.suspend.policy.should.be.set.to.all.threads")),
                    true,
                )
            }
        }
    }

    inner class CoroutineContainer(
        val suspendContext: SuspendContextImpl,
        val groupName: String
    ) : RendererContainer(renderer.renderGroup(groupName)) {

        override fun computeChildren(node: XCompositeNode) {
            managerThreadExecutor.on(suspendContext).schedule {
                val debugProbesProxy = CoroutineDebugProbesProxy(suspendContext)

                var coroutineCache = debugProbesProxy.dumpCoroutines()
                if (coroutineCache.isOk()) {
                    val children = XValueChildrenList()
                    for (coroutineInfo in coroutineCache.cache) {
                        children.add(FramesContainer(coroutineInfo, suspendContext))
                    }
                    if (children.size() > 0)
                        node.addChildren(children, true)
                    else
                        node.addChildren(XValueChildrenList.singleton(InfoNode("coroutine.view.fetching.not_found")), true)
                } else {
                    val errorNode = ErrorNode("coroutine.view.fetching.error")
                    node.addChildren(XValueChildrenList.singleton(errorNode), true)
                }
            }
        }
    }

    inner class InfoNode(val error: String) : RendererContainer(renderer.renderInfoNode(error))

    inner class ErrorNode(val error: String) : RendererContainer(renderer.renderErrorNode(error))

    inner class FramesContainer(
        private val infoData: CoroutineInfoData,
        private val suspendContext: SuspendContextImpl
    ) : RendererContainer(renderer.render(infoData)) {

        override fun computeChildren(node: XCompositeNode) {
            managerThreadExecutor.on(suspendContext).schedule {
                val debugProbesProxy = CoroutineDebugProbesProxy(suspendContext)
                val children = XValueChildrenList()
                var stackFrames = debugProbesProxy.frameBuilder().build(infoData)
                val creationStack = mutableListOf<CreationCoroutineStackFrameItem>()
                for (frame in stackFrames) {
                    if (frame is CreationCoroutineStackFrameItem)
                        creationStack.add(frame)
                    else if (frame is CoroutineStackFrameItem)
                        children.add(CoroutineFrameValue(infoData, frame))
                }
                if (creationStack.isNotEmpty())
                    children.add(CreationFramesContainer(infoData, creationStack))
                node.addChildren(children, true)
            }
        }
    }

    inner class CreationFramesContainer(
        private val infoData: CoroutineInfoData,
        private val creationFrames: List<CreationCoroutineStackFrameItem>
    ) : RendererContainer(renderer.renderCreationNode(infoData)) {

        override fun computeChildren(node: XCompositeNode) {
            val children = XValueChildrenList()

            creationFrames.forEach {
                children.add(CoroutineFrameValue(infoData, it))
            }
            node.addChildren(children, true)
        }
    }

    inner class CoroutineFrameValue(
        val infoData: CoroutineInfoData,
        val frameItem: CoroutineStackFrameItem
    ) : XNamedValue(frameItem.uniqueId()) {
        override fun computePresentation(node: XValueNode, place: XValuePlace) =
            applyRenderer(node, renderer.render(frameItem.location))
    }

    private fun applyRenderer(node: XValueNode, presentation: SimpleColoredTextIcon) =
        node.setPresentation(presentation.icon, presentation.valuePresentation(), presentation.hasChildrens)

    open inner class RendererContainer(val presentation: SimpleColoredTextIcon) : XNamedValue(presentation.simpleString()) {
        override fun computePresentation(node: XValueNode, place: XValuePlace) =
            applyRenderer(node, presentation)
    }

}
