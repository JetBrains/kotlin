/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.view

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.settings.ThreadsViewSettings
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleColoredText
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.SingleAlarm
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.frame.presentation.XRegularValuePresentation
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTreePanel
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueContainerNode
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.debugger.coroutines.CoroutineDebuggerContentInfo
import org.jetbrains.kotlin.idea.debugger.coroutines.CoroutineDebuggerContentInfo.Companion.XCOROUTINE_POPUP_ACTION_GROUP
import org.jetbrains.kotlin.idea.debugger.coroutines.command.CoroutineStackFrameItem
import org.jetbrains.kotlin.idea.debugger.coroutines.command.CreationCoroutineStackFrameItem
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutines.proxy.CoroutinesDebugProbesProxy
import org.jetbrains.kotlin.idea.debugger.coroutines.proxy.ManagerThreadExecutor
import org.jetbrains.kotlin.idea.debugger.coroutines.util.CreateContentParams
import org.jetbrains.kotlin.idea.debugger.coroutines.util.CreateContentParamsProvider
import org.jetbrains.kotlin.idea.debugger.coroutines.util.XDebugSessionListenerProvider
import org.jetbrains.kotlin.idea.debugger.coroutines.util.logger
import javax.swing.Icon


class XCoroutineView(val project: Project, val session: XDebugSession) :
    Disposable, XDebugSessionListenerProvider, CreateContentParamsProvider {
    val log by logger
    val splitter = OnePixelSplitter("SomeKey", 0.25f)
    val panel = XDebuggerTreePanel(project, session.debugProcess.editorsProvider, this, null, XCOROUTINE_POPUP_ACTION_GROUP, null)
    val alarm = SingleAlarm(Runnable { clear() }, VIEW_CLEAR_DELAY, this)
    val debugProcess: DebugProcessImpl = (session.debugProcess as JavaDebugProcess).debuggerSession.process

    companion object {
        private val VIEW_CLEAR_DELAY = 100 //ms
    }

    init {
        splitter.firstComponent = panel.mainPanel
    }

    fun clear() {
        DebuggerUIUtil.invokeLater {
            panel.tree
                .setRoot(object : XValueContainerNode<XValueContainer>(panel.tree, null, true, object : XValueContainer() {}) {}, false)
        }
    }

    override fun dispose() {
    }

    fun forceClear() {
        alarm.cancel()
        clear()
    }

    fun createRoot(suspendContext: XSuspendContext) =
        XCoroutinesRootNode(panel.tree, suspendContext)

    override fun debugSessionListener(session: XDebugSession) =
        CoroutineViewDebugSessionListener(session, this)

    override fun createContentParams(): CreateContentParams =
        CreateContentParams(
            CoroutineDebuggerContentInfo.XCOROUTINE_THREADS_CONTENT,
            splitter,
            KotlinBundle.message("debugger.session.tab.xcoroutine.title"),
            null,
            panel.tree
        )

}

class XCoroutinesRootNode(tree: XDebuggerTree, suspendContext: XSuspendContext) :
    XValueContainerNode<CoroutineGroupContainer>(tree, null, false, CoroutineGroupContainer(suspendContext)) {
}

class CoroutineGroupContainer(
    val suspendContext: XSuspendContext
) : XValueContainer() {
    override fun computeChildren(node: XCompositeNode) {
        val children = XValueChildrenList()
        children.add("", CoroutineContainer(suspendContext as SuspendContextImpl, "Default group"))
        node.addChildren(children, true)
    }
}

class CoroutineContainer(
    val suspendContext: SuspendContextImpl,
    val groupName: String
) : XValue() {
    override fun computeChildren(node: XCompositeNode) {
        val managerThreadExecutor = ManagerThreadExecutor(suspendContext.debugProcess)
        managerThreadExecutor.schedule {
            val debugProbesProxy = CoroutinesDebugProbesProxy(suspendContext)

            var coroutineCache = debugProbesProxy.dumpCoroutines()
            if(coroutineCache.isOk()) {
                val children = XValueChildrenList()
                coroutineCache.cache.forEach {
                    children.add("", FramesContainer(it, debugProbesProxy, managerThreadExecutor))
                }
                node.addChildren(children, true)
            } else
                node.addChildren(XValueChildrenList.EMPTY, true)
        }
    }

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        node.setPresentation(AllIcons.Debugger.ThreadGroup, XRegularValuePresentation(groupName, null, ""), true)
    }
}

class FramesContainer(
    private val coroutineInfoData: CoroutineInfoData,
    private val debugProbesProxy: CoroutinesDebugProbesProxy,
    private val managerThreadExecutor: ManagerThreadExecutor
) : XValue() {
    override fun computeChildren(node: XCompositeNode) {
        managerThreadExecutor.schedule {
            val children = XValueChildrenList()
            debugProbesProxy.frameBuilder().build(coroutineInfoData)
            val creationStack = mutableListOf<CreationCoroutineStackFrameItem>()
            coroutineInfoData.stackFrameList.forEach {
                val frameValue = when (it) {
                    is CreationCoroutineStackFrameItem -> {
                        creationStack.add(it)
                        null
                    }
                    else -> CoroutineFrameValue(it)
                }
                frameValue?.let {
                    children.add("", frameValue)
                }
            }
            children.add("", CreationFramesContainer(creationStack))
            node.addChildren(children, true)
        }
    }

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        val icon = when (coroutineInfoData.state) {
            CoroutineInfoData.State.SUSPENDED -> AllIcons.Debugger.ThreadSuspended
            CoroutineInfoData.State.RUNNING -> AllIcons.Debugger.ThreadRunning
            CoroutineInfoData.State.CREATED -> AllIcons.Debugger.ThreadStates.Idle
        }

        val valuePresentation = customizePresentation(coroutineInfoData)
        node.setPresentation(icon, valuePresentation, true)
    }

    private fun customizePresentation(coroutineInfoData: CoroutineInfoData): XRegularValuePresentation {
        val component = SimpleColoredComponent()
        val thread = coroutineInfoData.thread
        val name = thread?.name()?.substringBefore(" @${coroutineInfoData.name}") ?: ""
        val threadState = if (thread != null) DebuggerUtilsEx.getThreadStatusText(thread.status()) else ""

        component.append("\"").append(coroutineInfoData.name, XDebuggerUIConstants.VALUE_NAME_ATTRIBUTES)
        component.append("\": ${coroutineInfoData.state} ${if (name.isNotEmpty()) "on thread \"$name\":$threadState" else ""}")
        return XRegularValuePresentation(component.getCharSequence(false).toString(), null, "")
    }
}

class CreationFramesContainer(val creationFrames: List<CreationCoroutineStackFrameItem>) : XValue() {
    override fun computeChildren(node: XCompositeNode) {
        val children = XValueChildrenList()

        creationFrames.forEach {
            children.add("", CoroutineFrameValue(it))
        }
        node.addChildren(children, true)
    }

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        node.setPresentation(AllIcons.Debugger.ThreadSuspended, XRegularValuePresentation("Creation stack frame", null, ""), true)
    }
}

class CoroutineFrameValue(val frame: CoroutineStackFrameItem) : XValue() {
    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        val presentation = customizePresentation(frame)
        if(node is XValueNodeImpl) {
            node.setPresentation(null, XRegularValuePresentation("", null, ""), false)
            presentation.texts.forEachIndexed { i, s ->
                node.text.append(s, presentation.attributes[i])
            }
        } else {
            val component = SimpleColoredComponent()
            presentation.appendToComponent(component)
            val valuePresentation = XRegularValuePresentation(component.getCharSequence(false).toString(), null, "")
            node.setPresentation(null, valuePresentation, false)
        }
    }
}

fun customizePresentation(frame: CoroutineStackFrameItem) : SimpleColoredText =
   calcRepresentation(frame.location(), ThreadsViewSettings.getInstance())


/**
 * Taken from #StackFrameDescriptorImpl.calcRepresentation
 */
fun calcRepresentation(location: Location, settings: ThreadsViewSettings): SimpleColoredText {
    val label = SimpleColoredText()
    DebuggerUIUtil.getColorScheme(null)
    if (location.method() != null) {
        val myName = location.method().name()
        label.append(if (settings.SHOW_ARGUMENTS_TYPES) DebuggerUtilsEx.methodNameWithArguments(location.method()) else myName, XDebuggerUIConstants.VALUE_NAME_ATTRIBUTES)
    }
    if (settings.SHOW_LINE_NUMBER) {
        label.append(":", SimpleTextAttributes.REGULAR_ATTRIBUTES)
        label.append("" + DebuggerUtilsEx.getLineNumber(location, false), SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }
    if (settings.SHOW_CLASS_NAME) {
        val name: String?
        name = try {
            val refType: ReferenceType = location.declaringType()
            refType?.name()
        } catch (e: InternalError) {
            e.toString()
        }
        if (name != null) {
            label.append(", ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
            val dotIndex = name.lastIndexOf('.')
            if (dotIndex < 0) {
                label.append(name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            } else {
                label.append(name.substring(dotIndex + 1), SimpleTextAttributes.REGULAR_ATTRIBUTES)
                if (settings.SHOW_PACKAGE_NAME) {
                    label.append(" (${name.substring( 0, dotIndex)})", SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }
        }
    }
    if (settings.SHOW_SOURCE_NAME) {
        label.append(", ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
        label.append(DebuggerUtilsEx.getSourceName(location) { e: Throwable? -> "Unknown Source" }, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }
    return label
}
