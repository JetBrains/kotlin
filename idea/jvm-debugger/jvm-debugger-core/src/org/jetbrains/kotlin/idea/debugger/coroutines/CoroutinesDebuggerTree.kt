/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines

import com.intellij.debugger.DebuggerBundle
import com.intellij.debugger.DebuggerInvocationUtil
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.actions.GotoFrameSourceAction
import com.intellij.debugger.engine.*
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.engine.events.DebuggerCommandImpl
import com.intellij.debugger.engine.events.SuspendContextCommandImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.debugger.ui.impl.tree.TreeBuilder
import com.intellij.debugger.ui.impl.tree.TreeBuilderNode
import com.intellij.debugger.ui.impl.watch.*
import com.intellij.debugger.ui.tree.StackFrameDescriptor
import com.intellij.ide.DataManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.DoubleClickListener
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.impl.XDebuggerManagerImpl
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import com.sun.jdi.ClassType
import javaslang.control.Either
import org.jetbrains.kotlin.idea.debugger.AsyncStackTraceContext
import org.jetbrains.kotlin.idea.debugger.KotlinCoroutinesAsyncStackTraceProvider
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext
import java.awt.event.MouseEvent
import java.lang.ref.WeakReference
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener

/**
 * Tree of coroutines for [CoroutinesPanel]
 */
class CoroutinesDebuggerTree(project: Project) : DebuggerTree(project) {
    private val logger = Logger.getInstance(this::class.java)
    private var lastSuspendContextCache: Cache? = null

    override fun createNodeManager(project: Project): NodeManagerImpl {
        return object : NodeManagerImpl(project, this) {
            override fun getContextKey(frame: StackFrameProxyImpl?): String? {
                return "CoroutinesView"
            }
        }
    }

    /**
     * Prepare specific behavior instead of DebuggerTree constructor
     */
    init {
        val model = object : TreeBuilder(this) {
            override fun buildChildren(node: TreeBuilderNode) {
                val debuggerTreeNode = node as DebuggerTreeNodeImpl
                if (debuggerTreeNode.descriptor is DefaultNodeDescriptor) {
                    return
                }

                node.add(myNodeManager.createMessageNode(MessageDescriptor.EVALUATING))
                buildNode(debuggerTreeNode)
            }

            override fun isExpandable(builderNode: TreeBuilderNode): Boolean {
                return this@CoroutinesDebuggerTree.isExpandable(builderNode as DebuggerTreeNodeImpl)
            }
        }
        model.setRoot(nodeFactory.defaultNode)
        model.addTreeModelListener(createListener())

        setModel(model)
        emptyText.text = "Coroutines are not available"
    }

    /**
     * Add frames inside coroutine (node)
     */
    private fun buildNode(node: DebuggerTreeNodeImpl) {
        val context = DebuggerManagerEx.getInstanceEx(project).context
        val debugProcess = context.debugProcess
        debugProcess?.managerThread?.schedule(object : SuspendContextCommandImpl(context.suspendContext) {
            override fun contextAction(suspendContext: SuspendContextImpl) {
                val evalContext = debuggerContext.createEvaluationContext() ?: return
                if (node.descriptor is CoroutineDescriptorImpl || node.descriptor is CreationFramesDescriptor) {
                    val children = mutableListOf<DebuggerTreeNodeImpl>()
                    try {
                        addChildren(children, debugProcess, node.descriptor, evalContext)
                    } catch (e: EvaluateException) {
                        children.clear()
                        children.add(myNodeManager.createMessageNode(e.message))
                        logger.debug(e)
                    }
                    DebuggerInvocationUtil.swingInvokeLater(project) {
                        node.removeAllChildren()
                        for (debuggerTreeNode in children) {
                            node.add(debuggerTreeNode)
                        }
                        node.childrenChanged(true)
                    }
                }
            }
        })
    }

    fun installAction(): () -> Unit {
        val listener = object : DoubleClickListener() {
            override fun onDoubleClick(e: MouseEvent): Boolean {
                val location = getPathForLocation(e.x, e.y)
                    ?.lastPathComponent as? DebuggerTreeNodeImpl ?: return false
                return selectFrame(location.userObject)
            }
        }
        listener.installOn(this)

        return { listener.uninstall(this) }
    }

    fun selectFrame(descriptor: Any): Boolean {
        val dataContext = DataManager.getInstance().getDataContext(this@CoroutinesDebuggerTree)
        val context = DebuggerManagerEx.getInstanceEx(project).context
        when (descriptor) {
            is SuspendStackFrameDescriptor -> {
                buildSuspendStackFrameChildren(descriptor)
                return true
            }
            is AsyncStackFrameDescriptor -> {
                buildAsyncStackFrameChildren(descriptor, context.debugProcess ?: return false)
                return true
            }
            is EmptyStackFrameDescriptor -> {
                buildEmptyStackFrameChildren(descriptor)
                return true
            }
            is StackFrameDescriptor -> {
                GotoFrameSourceAction.doAction(dataContext)
                return true
            }
            else -> return true
        }
    }

    private fun buildSuspendStackFrameChildren(descriptor: SuspendStackFrameDescriptor) {
        val context = DebuggerManagerEx.getInstanceEx(project).context
        val pos = getPosition(descriptor.frame) ?: return
        context.debugProcess?.managerThread?.schedule(object : SuspendContextCommandImpl(context.suspendContext) {
            override fun contextAction() {
                val (stack, stackFrame) = createSyntheticStackFrame(descriptor, pos) ?: return
                val action: () -> Unit = { context.debuggerSession?.xDebugSession?.setCurrentStackFrame(stack, stackFrame) }
                ApplicationManager.getApplication()
                    .invokeLater(action, ModalityState.stateForComponent(this@CoroutinesDebuggerTree))
            }
        })
    }

    private fun buildAsyncStackFrameChildren(descriptor: AsyncStackFrameDescriptor, process: DebugProcessImpl) {
        process.managerThread?.schedule(object : DebuggerCommandImpl() {
            override fun action() {
                val context = DebuggerManagerEx.getInstanceEx(project).context
                val proxy = ThreadReferenceProxyImpl(
                    process.virtualMachineProxy,
                    descriptor.state.thread // is not null because it's a running coroutine
                )
                val executionStack = JavaExecutionStack(proxy, process, false)
                executionStack.initTopFrame()
                val frame = descriptor.frame.createFrame(process)
                DebuggerUIUtil.invokeLater {
                    context.debuggerSession?.xDebugSession?.setCurrentStackFrame(
                        executionStack,
                        frame
                    )
                }
            }
        })
    }

    private fun buildEmptyStackFrameChildren(descriptor: EmptyStackFrameDescriptor) {
        val position = getPosition(descriptor.frame) ?: return
        val context = DebuggerManagerEx.getInstanceEx(project).context
        val suspendContext = context.suspendContext ?: return
        val proxy = suspendContext.thread ?: return
        context.debugProcess?.managerThread?.schedule(object : DebuggerCommandImpl() {
            override fun action() {
                val executionStack =
                    JavaExecutionStack(proxy, context.debugProcess!!, false)
                executionStack.initTopFrame()
                val frame = SyntheticStackFrame(descriptor, emptyList(), position)
                val action: () -> Unit =
                    { context.debuggerSession?.xDebugSession?.setCurrentStackFrame(executionStack, frame) }
                ApplicationManager.getApplication()
                    .invokeLater(action, ModalityState.stateForComponent(this@CoroutinesDebuggerTree))
            }
        })
    }

    private fun getPosition(frame: StackTraceElement): XSourcePosition? {

        val psiFacade = JavaPsiFacade.getInstance(project)
        val psiClass = psiFacade.findClass(
            frame.className.substringBefore("$"), // find outer class, for which psi exists TODO
            GlobalSearchScope.everythingScope(project)
        )
        val classFile = psiClass?.containingFile?.virtualFile
        // to convert to 0-based line number or '-1' to do not move
        val lineNumber = if(frame.lineNumber > 0) frame.lineNumber - 1 else return null
        return XDebuggerUtil.getInstance().createPosition(classFile,  lineNumber)
    }

    /**
     * Should be invoked on manager thread
     */
    private fun createSyntheticStackFrame(
        descriptor: SuspendStackFrameDescriptor,
        pos: XSourcePosition
    ): Pair<XExecutionStack, SyntheticStackFrame>? {
        val context = DebuggerManagerEx.getInstanceEx(project).context
        val suspendContext = context.suspendContext ?: return null
        val proxy = suspendContext.thread ?: return null
        val executionStack = JavaExecutionStack(proxy, suspendContext.debugProcess, false)
        executionStack.initTopFrame()
        val evalContext = context.createEvaluationContext()
        val frameProxy = evalContext?.frameProxy ?: return null
        val execContext = ExecutionContext(evalContext, frameProxy)
        val continuation = descriptor.continuation // guaranteed that it is a BaseContinuationImpl
        val aMethod = (continuation.type() as ClassType).concreteMethodByName(
            "getStackTraceElement",
            "()Ljava/lang/StackTraceElement;"
        )
        val debugMetadataKtType = execContext
            .findClass("kotlin.coroutines.jvm.internal.DebugMetadataKt") as ClassType
        val vars = with(KotlinCoroutinesAsyncStackTraceProvider()) {
            AsyncStackTraceContext(
                execContext,
                aMethod,
                debugMetadataKtType
            ).getSpilledVariables(continuation)
        } ?: return null
        return executionStack to SyntheticStackFrame(descriptor, vars, pos)
    }

    private fun addChildren(
        children: MutableList<DebuggerTreeNodeImpl>,
        debugProcess: DebugProcessImpl,
        descriptor: NodeDescriptorImpl,
        evalContext: EvaluationContextImpl
    ) {
        val creationStackTraceSeparator = "\b\b\b" // the "\b\b\b" is used for creation stacktrace separator in kotlinx.coroutines
        if (descriptor !is CoroutineDescriptorImpl) {
            if (descriptor is CreationFramesDescriptor) {
                val threadProxy = debuggerContext.suspendContext?.thread ?: return
                val proxy = threadProxy.forceFrames().first()
                descriptor.frames.forEach {
                    children.add(myNodeManager.createNode(EmptyStackFrameDescriptor(it, proxy), evalContext))
                }
            }
            return
        }
        when (descriptor.state.state) {
            CoroutineState.State.RUNNING -> {
                if (descriptor.state.thread == null) {
                    children.add(myNodeManager.createMessageNode("Frames are not available"))
                    return
                }
                val proxy = ThreadReferenceProxyImpl(
                    debugProcess.virtualMachineProxy,
                    descriptor.state.thread
                )
                val frames = proxy.forceFrames()
                var i = frames.lastIndex
                while (i > 0 && frames[i].location().method().name() != "resumeWith") i--
                // if i is less than 0, wait, what?
                for (frame in 0..--i) {
                    children.add(createFrameDescriptor(descriptor, evalContext, frames[frame]))
                }
                if (i > 0) { // add async stack trace if there are frames after invokeSuspend
                    val async = KotlinCoroutinesAsyncStackTraceProvider().getAsyncStackTrace(
                        JavaStackFrame(StackFrameDescriptorImpl(frames[i - 1], MethodsTracker()), true),
                        evalContext.suspendContext
                    )
                    async?.forEach { children.add(createAsyncFrameDescriptor(descriptor, evalContext, it, frames[0])) }
                }
                for (frame in i + 2..frames.lastIndex) {
                    children.add(createFrameDescriptor(descriptor, evalContext, frames[frame]))
                }
            }
            CoroutineState.State.SUSPENDED -> {
                val threadProxy = debuggerContext.suspendContext?.thread ?: return
                val proxy = threadProxy.forceFrames().first()
                // the thread is paused on breakpoint - it has at least one frame
                for (it in descriptor.state.stackTrace) {
                    if (it.className.startsWith(creationStackTraceSeparator)) break
                    children.add(createCoroutineFrameDescriptor(descriptor, evalContext, it, proxy))
                }
            }
            else -> {
            }
        }
        val trace = descriptor.state.stackTrace
        val index = trace.indexOfFirst { it.className.startsWith(creationStackTraceSeparator) }
        children.add(myNodeManager.createNode(CreationFramesDescriptor(trace.subList(index + 1, trace.size)), evalContext))
    }


    private fun createFrameDescriptor(
        descriptor: NodeDescriptorImpl,
        evalContext: EvaluationContextImpl,
        frame: StackFrameProxyImpl
    ): DebuggerTreeNodeImpl {
        return myNodeManager.createNode(
            myNodeManager.getStackFrameDescriptor(descriptor, frame),
            evalContext
        )
    }

    private fun createCoroutineFrameDescriptor(
        descriptor: CoroutineDescriptorImpl,
        evalContext: EvaluationContextImpl,
        frame: StackTraceElement,
        proxy: StackFrameProxyImpl,
        parent: NodeDescriptorImpl? = null
    ): DebuggerTreeNodeImpl {
        return myNodeManager.createNode(
            myNodeManager.getDescriptor(
                parent,
                CoroutineStackFrameData(descriptor.state, frame, proxy)
            ), evalContext
        )
    }

    private fun createAsyncFrameDescriptor(
        descriptor: CoroutineDescriptorImpl,
        evalContext: EvaluationContextImpl,
        frame: StackFrameItem,
        proxy: StackFrameProxyImpl
    ): DebuggerTreeNodeImpl {
        return myNodeManager.createNode(
            myNodeManager.getDescriptor(
                descriptor,
                CoroutineStackFrameData(descriptor.state, frame, proxy)
            ), evalContext
        )
    }

    private fun createListener() = object : TreeModelListener {
        override fun treeNodesChanged(event: TreeModelEvent) {
            hideTooltip()
        }

        override fun treeNodesInserted(event: TreeModelEvent) {
            hideTooltip()
        }

        override fun treeNodesRemoved(event: TreeModelEvent) {
            hideTooltip()
        }

        override fun treeStructureChanged(event: TreeModelEvent) {
            hideTooltip()
        }
    }

    override fun isExpandable(node: DebuggerTreeNodeImpl): Boolean {
        val descriptor = node.descriptor
        return if (descriptor is StackFrameDescriptor) false else descriptor.isExpandable
    }

    override fun build(context: DebuggerContextImpl) {
        val session = context.debuggerSession
        val command = RefreshCoroutinesTreeCommand(session, context.suspendContext)

        val state = if (session != null) session.state else DebuggerSession.State.DISPOSED
        if (ApplicationManager.getApplication().isUnitTestMode
            || state == DebuggerSession.State.PAUSED
        ) {
            showMessage(MessageDescriptor.EVALUATING)
            context.debugProcess!!.managerThread.schedule(command)
        } else {
            showMessage(if (session != null) session.stateDescription else DebuggerBundle.message("status.debug.stopped"))
        }
    }

    private inner class RefreshCoroutinesTreeCommand(private val mySession: DebuggerSession?, context: SuspendContextImpl?) :
        SuspendContextCommandImpl(context) {

        override fun contextAction() {
            val root = nodeFactory.defaultNode
            mySession ?: return
            val suspendContext = suspendContext
            if (suspendContext == null || suspendContext.isResumed) {
                setRoot(root.apply { add(myNodeManager.createMessageNode("Application is resumed")) })
                return
            }
            val evaluationContext = EvaluationContextImpl(suspendContext, suspendContext.frameProxy)
            val executionContext = ExecutionContext(evaluationContext, suspendContext.frameProxy ?: return)
            val cache = lastSuspendContextCache
            val states = if (cache != null && cache.first.get() === suspendContext) {
                cache.second
            } else CoroutinesDebugProbesProxy.dumpCoroutines(executionContext).apply {
                lastSuspendContextCache = WeakReference(suspendContext) to this
            }
            // if suspend context hasn't changed - use last dump, else compute new
            if (states.isLeft) {
                logger.warn(states.left)
                setRoot(root.apply {
                    clear()
                    add(nodeFactory.createMessageNode(MessageDescriptor("Dump failed")))
                })
                XDebuggerManagerImpl.NOTIFICATION_GROUP
                    .createNotification(
                        "Coroutine dump failed. See log",
                        MessageType.ERROR
                    ).notify(project)
                return
            }
            for (state in states.get()) {
                root.add(
                    nodeFactory.createNode(
                        nodeFactory.getDescriptor(null, CoroutineData(state)), evaluationContext
                    )
                )
            }
            setRoot(root)
        }

        private fun setRoot(root: DebuggerTreeNodeImpl) {
            DebuggerInvocationUtil.swingInvokeLater(project) {
                mutableModel.setRoot(root)
                treeChanged()
            }
        }

    }

}

private typealias Cache = Pair<WeakReference<SuspendContextImpl>, Either<Throwable, List<CoroutineState>>>
