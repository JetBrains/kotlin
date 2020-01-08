/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.view

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.debugger.engine.JavaExecutionStack
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.ide.CommonActionsManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.CaptionPanel
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.border.CustomLineBorder
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.SingleAlarm
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.impl.actions.XDebuggerActions
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTreePanel
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTreeRestorer
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTreeState
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueContainerNode
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl
import javaslang.control.Either
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.debugger.coroutine.CoroutineDebuggerContentInfo
import org.jetbrains.kotlin.idea.debugger.coroutine.CoroutineDebuggerContentInfo.Companion.XCOROUTINE_POPUP_ACTION_GROUP
import org.jetbrains.kotlin.idea.debugger.coroutine.VersionedImplementationProvider
import org.jetbrains.kotlin.idea.debugger.coroutine.command.*
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutine.data.SyntheticStackFrame
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.*
import org.jetbrains.kotlin.idea.debugger.coroutine.util.CreateContentParams
import org.jetbrains.kotlin.idea.debugger.coroutine.util.CreateContentParamsProvider
import org.jetbrains.kotlin.idea.debugger.coroutine.util.XDebugSessionListenerProvider
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.JPanel


class XCoroutineView(val project: Project, val session: XDebugSession) :
    Disposable, XDebugSessionListenerProvider, CreateContentParamsProvider {
    val log by logger
    val versionedImplementationProvider = VersionedImplementationProvider()

    val mainPanel = JPanel(BorderLayout())
    val someCombobox = ComboBox<String>()
    val panel = XDebuggerTreePanel(project, session.debugProcess.editorsProvider, this, null, XCOROUTINE_POPUP_ACTION_GROUP, null)
    val alarm = SingleAlarm(Runnable { resetRoot() }, VIEW_CLEAR_DELAY, this)
    val javaDebugProcess = session.debugProcess as JavaDebugProcess
    val debugProcess: DebugProcessImpl = javaDebugProcess.debuggerSession.process
    val renderer = SimpleColoredTextIconPresentationRenderer()
    val managerThreadExecutor = ManagerThreadExecutor(debugProcess)
    val applicationThreadExecutor = ApplicationThreadExecutor()
    var treeState: XDebuggerTreeState? = null
    private var restorer: XDebuggerTreeRestorer? = null
    private var selectedNodeListener =  XDebuggerTreeSelectedNodeListener(panel.tree)

    companion object {
        private val VIEW_CLEAR_DELAY = 100 //ms
    }

    init {
        someCombobox.setRenderer(versionedImplementationProvider.comboboxListCellRenderer())
        object : ComboboxSpeedSearch(someCombobox) {
            override fun getElementText(element: Any?): String? {
                return element.toString()
            }
        }
        someCombobox.addItem(null)
        val myToolbar = createToolbar()
        val myThreadsPanel = Wrapper()
        myThreadsPanel.setBorder(CustomLineBorder(CaptionPanel.CNT_ACTIVE_BORDER_COLOR, 0, 0, 1, 0))
        myThreadsPanel.add(myToolbar?.getComponent(), BorderLayout.EAST)
        myThreadsPanel.add(someCombobox, BorderLayout.CENTER)
        mainPanel.add(myThreadsPanel, BorderLayout.NORTH)
        mainPanel.add(panel.mainPanel, BorderLayout.CENTER)
        selectedNodeListener.installOn()
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
            if (! (panel.tree.root is EmptyNode)) {
                treeState = XDebuggerTreeState.saveState(panel.tree)
                log.info("Tree state saved")
            }
        }
    }

    fun resetRoot() {
        DebuggerUIUtil.invokeLater {
            panel.tree.setRoot(EmptyNode(), false)
        }
    }

    fun renewRoot(suspendContext: XSuspendContext) {
        panel.tree.setRoot(XCoroutinesRootNode(suspendContext), false)
        if(treeState != null) {
            restorer?.dispose()
            restorer = treeState?.restoreState(panel.tree)
            log.info("Tree state restored")
        }
    }

    override fun dispose() {
        restorer?.dispose()
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
            KotlinBundle.message("debugger.session.tab.xcoroutine.title"),
            null,
            panel.tree
        )

    inner class EmptyNode : XValueContainerNode<XValueContainer>(panel.tree, null, true, object : XValueContainer() {})

    inner class XCoroutinesRootNode(suspendContext: XSuspendContext) :
        XValueContainerNode<CoroutineGroupContainer>(panel.tree, null, false, CoroutineGroupContainer(suspendContext, "Default group"))

    inner class CoroutineGroupContainer(val suspendContext: XSuspendContext, val groupName: String) : XValueContainer() {
        override fun computeChildren(node: XCompositeNode) {
            val groups = XValueChildrenList.singleton(CoroutineContainer(suspendContext, groupName))
            node.addChildren(groups, true)
        }
    }

    inner class CoroutineContainer(
        val suspendContext: XSuspendContext,
        val groupName: String
    ) : RendererContainer(renderer.renderGroup(groupName)) {

        override fun computeChildren(node: XCompositeNode) {
            managerThreadExecutor.on(suspendContext).schedule {
                val debugProbesProxy = CoroutineDebugProbesProxy(suspendContext)

                var coroutineCache = debugProbesProxy.dumpCoroutines()
                if (coroutineCache.isOk()) {
                    val children = XValueChildrenList()
                    coroutineCache.cache.forEach {
                        children.add(FramesContainer(it, suspendContext))
                    }
                    node.addChildren(children, true)
                } else {
                    node.addChildren(XValueChildrenList.singleton(ErrorNode("Error occurs while fetching information")), true)
                }
            }
        }
    }

    inner class ErrorNode(val error: String) : RendererContainer(renderer.renderErrorNode(error))

    inner class FramesContainer(
        private val infoData: CoroutineInfoData,
        private val suspendContext: XSuspendContext
    ) : RendererContainer(renderer.render(infoData)) {

        override fun computeChildren(node: XCompositeNode) {
            managerThreadExecutor.on(suspendContext).schedule {
                val debugProbesProxy = CoroutineDebugProbesProxy(suspendContext)
                val children = XValueChildrenList()
                debugProbesProxy.frameBuilder().build(infoData)
                val creationStack = mutableListOf<CreationCoroutineStackFrameItem>()
                infoData.stackFrameList.forEach {
                    if (it is CreationCoroutineStackFrameItem)
                        creationStack.add(it)
                    else
                        children.add(CoroutineFrameValue(it))
                }
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
                children.add(CoroutineFrameValue(it))
            }
            node.addChildren(children, true)
        }
    }

    inner class CoroutineFrameValue(val frame: CoroutineStackFrameItem
    ) : XNamedValue(frame.uniqueId()) {
        override fun computePresentation(node: XValueNode, place: XValuePlace) =
            applyRenderer(node, renderer.render(frame.location()))
    }

    private fun applyRenderer(node: XValueNode, presentation: SimpleColoredTextIcon) =
        node.setPresentation(presentation.icon, presentation.valuePresentation(), presentation.hasChildrens)

    open inner class RendererContainer(val presentation: SimpleColoredTextIcon) : XNamedValue(presentation.simpleString()) {
        override fun computePresentation(node: XValueNode, place: XValuePlace) =
            applyRenderer(node, presentation)
    }

    data class KeyMouseEvent(val keyEvent: KeyEvent?, val mouseEvent: MouseEvent?) {
        constructor(keyEvent: KeyEvent) : this(keyEvent, null)
        constructor(mouseEvent: MouseEvent) : this(null, mouseEvent)
        
        fun isKeyEvent() = keyEvent != null

        fun isMouseEvent() = mouseEvent != null
    }

    inner class XDebuggerTreeSelectedNodeListener(val tree: XDebuggerTree) {

        fun installOn() {
            object : DoubleClickListener() {
                override fun onDoubleClick(e: MouseEvent) =
                    nodeSelected(KeyMouseEvent(e))
            }.installOn(tree)

            tree.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    val key = e.keyCode
                    if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE || key == KeyEvent.VK_RIGHT)
                        nodeSelected(KeyMouseEvent(e))
                }
            })
        }

        fun nodeSelected(event: KeyMouseEvent) : Boolean {
            val selectedNodes = tree.getSelectedNodes(XValueNodeImpl::class.java, null)
            if (selectedNodes.size == 1) {
                val node = selectedNodes[0]
                val valueContainer = node.valueContainer
                if (valueContainer is XCoroutineView.CoroutineFrameValue) {
                    val frame = valueContainer.frame
                    val threadSuspendContext = session.suspendContext as SuspendContextImpl
                    when (frame) {
                        is RunningCoroutineStackFrameItem -> {
                            val threadProxy = valueContainer.frame.frame.threadProxy()
                            val isCurrentContext = threadSuspendContext.thread == threadProxy
                            createStackAndSetFrame(threadProxy, { frame.stackFrame }, isCurrentContext)
                        }
                        is CreationCoroutineStackFrameItem -> {
                            val position = getPosition(frame.stackTraceElement) ?: return false
                            val threadProxy = threadSuspendContext.thread as ThreadReferenceProxyImpl
                            createStackAndSetFrame(threadProxy, { SyntheticStackFrame(frame.emptyDescriptor(), emptyList(), position) })
                        }
                        is SuspendCoroutineStackFrameItem -> {
                            val threadProxy = threadSuspendContext.thread as ThreadReferenceProxyImpl
                            val executionContext = executionContext(threadSuspendContext, frame.frame)
                            createStackAndSetFrame(threadProxy, { createSyntheticStackFrame(executionContext, frame) })
                        }
                        is AsyncCoroutineStackFrameItem -> {
                        }
                        else -> {
                        }
                    }
                }
            }
            return false
        }
    }

    fun createStackAndSetFrame(threadReferenceProxy: ThreadReferenceProxyImpl, stackFrameProvider: () -> XStackFrame?, isCurrentContext: Boolean = false) {
        val threadSuspendContext = session.suspendContext as SuspendContextImpl
        managerThreadExecutor.on(threadSuspendContext).schedule {
            val stackFrame = stackFrameProvider.invoke()
            if(stackFrame is XStackFrame) {
                val executionStack = createExecutionStack(threadReferenceProxy, isCurrentContext)
                applicationThreadExecutor.schedule(
                    {
                        session.setCurrentStackFrame(executionStack, stackFrame)
                    }, panel.tree
                )
            }
        }
    }

    private fun createExecutionStack(proxy: ThreadReferenceProxyImpl, isCurrentContext: Boolean = false) : XExecutionStack {
        val executionStack = CoroutineDebuggerExecutionStack(proxy, isCurrentContext)
        executionStack.initTopFrame()
        return executionStack
    }

    inner class CoroutineDebuggerExecutionStack(threadReferenceProxy: ThreadReferenceProxyImpl, isCurrentContext: Boolean) :
        JavaExecutionStack(threadReferenceProxy, debugProcess, isCurrentContext)


    private fun getPosition(frame: StackTraceElement): XSourcePosition? {
        val psiFacade = JavaPsiFacade.getInstance(project)
        val psiClass = psiFacade.findClass(
            frame.className.substringBefore("$"), // find outer class, for which psi exists TODO
            GlobalSearchScope.everythingScope(project)
        )
        val classFile = psiClass?.containingFile?.virtualFile
        // to convert to 0-based line number or '-1' to do not move
        val lineNumber = if (frame.lineNumber > 0) frame.lineNumber - 1 else return null
        return XDebuggerUtil.getInstance().createPosition(classFile, lineNumber)
    }

    private fun executionContext(suspendContext: SuspendContextImpl, frameProxy: StackFrameProxyImpl) :  ExecutionContext {
        val evaluationContextImpl = EvaluationContextImpl(suspendContext, frameProxy)
        return ExecutionContext(evaluationContextImpl, frameProxy)
    }

    private fun createSyntheticStackFrame(
        executionContext: ExecutionContext,
        frame: SuspendCoroutineStackFrameItem
    ): SyntheticStackFrame? {
        val position = getPosition(frame.stackTraceElement) ?: return null
        val lookupContinuation = LookupContinuation(executionContext, frame.stackTraceElement)
        val continuation = lookupContinuation.findContinuation(frame.lastObservedFrameFieldRef) ?: return null

        val asyncStackTraceContext = lookupContinuation.createAsyncStackTraceContext(continuation)
        val vars = asyncStackTraceContext?.getSpilledVariables(continuation) ?: return null
        return SyntheticStackFrame(frame.emptyDescriptor(), vars, position)
    }
}

