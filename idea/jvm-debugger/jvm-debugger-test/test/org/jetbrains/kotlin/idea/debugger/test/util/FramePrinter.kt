/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.util

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.*
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import com.intellij.debugger.engine.evaluation.TextWithImports
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.debugger.settings.NodeRendererSettings
import com.intellij.debugger.ui.impl.watch.*
import com.intellij.debugger.ui.tree.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiExpression
import com.intellij.xdebugger.impl.XDebugSessionImpl
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl
import com.intellij.xdebugger.impl.frame.XDebugViewSessionListener
import com.intellij.xdebugger.impl.frame.XVariablesView
import com.intellij.xdebugger.impl.frame.XWatchesViewImpl
import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree
import com.intellij.xdebugger.impl.ui.tree.nodes.*
import org.jetbrains.kotlin.idea.debugger.KotlinFrameExtraVariablesProvider
import org.jetbrains.kotlin.idea.debugger.coroutine.data.ContinuationVariableValueDescriptorImpl
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinCodeFragmentFactory
import org.jetbrains.kotlin.idea.debugger.invokeInManagerThread
import org.jetbrains.kotlin.idea.debugger.test.KOTLIN_LIBRARY_NAME
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferenceKeys
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferences
import org.jetbrains.kotlin.idea.debugger.test.util.PrinterConfig.DescriptorViewOptions
import org.jetbrains.kotlin.psi.KtFile
import java.io.Closeable
import javax.swing.tree.TreeNode

class FramePrinter(
    debuggerSession: DebuggerSession,
    private val delegate: FramePrinterDelegate,
    private val preferences: DebuggerPreferences,
    private val testRootDisposable: Disposable
) : Closeable {
    private companion object {
        fun getClassRenderer() = NodeRendererSettings.getInstance()!!.classRenderer!!
    }

    private lateinit var variablesView: XVariablesView
    private lateinit var watchesView: XWatchesViewImpl

    private val oldShowFqTypeNames: Boolean

    init {
        ApplicationManager.getApplication().invokeAndWait(
            {
                variablesView = createVariablesView(debuggerSession)
                watchesView = createWatchesView(debuggerSession)
            }, ModalityState.any()
        )

        getClassRenderer().let { renderer ->
            oldShowFqTypeNames = renderer.SHOW_FQ_TYPE_NAMES
            renderer.SHOW_FQ_TYPE_NAMES = true
        }
    }

    override fun close() {
        getClassRenderer().SHOW_FQ_TYPE_NAMES = oldShowFqTypeNames
    }

    fun printFrame(completion: () -> Unit) {
        if (preferences[DebuggerPreferenceKeys.PRINT_FRAME]) {
            doPrintFrame(completion)
        } else {
            completion()
        }
    }

    private fun doPrintFrame(completion: () -> Unit) {
        val tree = variablesView.tree

        val config = getPrinterConfig()

        fun processor() {
            Printer(delegate, config).printTree(tree)

            for (extra in getExtraVars()) {
                watchesView.addWatchExpression(XExpressionImpl.fromText(extra.text), -1, false)
            }

            Printer(delegate, config).printTree(watchesView.tree)

            completion()
        }

        // TODO why this is needed? Otherwise some tests are never ended
        val filter: (TreeNode) -> Boolean = { it !is XValueNodeImpl || it.name != "cause" }

        delegate.expandAll(tree, ::processor, filter, delegate.evaluationContext.suspendContext)
    }

    private fun getPrinterConfig(): PrinterConfig {
        val skipInPrintFrame = preferences[DebuggerPreferenceKeys.SKIP].flatMap { it.split(',') }.map { it.trim() }
        val viewOptions = DescriptorViewOptions.valueOf(preferences[DebuggerPreferenceKeys.DESCRIPTOR_VIEW_OPTIONS])
        return PrinterConfig(skipInPrintFrame, viewOptions)
    }

    private fun createWatchesView(debuggerSession: DebuggerSession): XWatchesViewImpl {
        val session = debuggerSession.xDebugSession as XDebugSessionImpl
        val watchesView = XWatchesViewImpl(session, false)
        Disposer.register(testRootDisposable, watchesView)
        XDebugViewSessionListener.attach(watchesView, session)
        return watchesView
    }

    private fun createVariablesView(debuggerSession: DebuggerSession): XVariablesView {
        val session = debuggerSession.xDebugSession as XDebugSessionImpl
        val variablesView = XVariablesView(session)
        Disposer.register(testRootDisposable, variablesView)
        XDebugViewSessionListener.attach(variablesView, session)
        return variablesView
    }

    private fun getExtraVars(): Set<TextWithImports> {
        return KotlinFrameExtraVariablesProvider()
            .collectVariables(delegate.debuggerContext.sourcePosition, delegate.evaluationContext, hashSetOf())
    }
}

private class PrinterConfig(
    val variablesToSkipInPrintFrame: List<String> = emptyList(),
    val viewOptions: DescriptorViewOptions = DescriptorViewOptions.FULL
) {
    enum class DescriptorViewOptions {
        FULL, NAME_EXPRESSION, NAME_EXPRESSION_RESULT
    }

    fun shouldRenderSourcesPosition(): Boolean {
        return when (viewOptions) {
            DescriptorViewOptions.FULL -> true
            else -> false
        }
    }

    fun shouldRenderExpression(): Boolean {
        return when {
            viewOptions.toString().contains("EXPRESSION") -> true
            else -> false
        }
    }

    fun renderLabel(node: TreeNode, descriptor: NodeDescriptorImpl): String {
        return when {
            descriptor is WatchItemDescriptor -> descriptor.calcValueName()
            viewOptions.toString().contains("NAME") -> (node as? XValueNodeImpl)?.name ?: descriptor.name ?: descriptor.label
            else -> descriptor.label
        }
    }

    fun shouldComputeResultOfCreateExpression(): Boolean {
        return viewOptions == DescriptorViewOptions.NAME_EXPRESSION_RESULT
    }
}

private class Printer(private val delegate: FramePrinterDelegate, private val config: PrinterConfig) {
    fun printTree(tree: XDebuggerTree) {
        val root = tree.treeModel.root as TreeNode
        printNode(root, 0)
    }

    private fun printNode(node: TreeNode, indent: Int) {
        val project = delegate.debuggerContext.project

        val descriptor = when (node) {
            is DebuggerTreeNodeImpl -> node.descriptor
            is XValueNodeImpl -> (node.valueContainer as? JavaValue)?.descriptor ?: MessageDescriptor(node.text.toString())
            is XStackFrameNode -> (node.valueContainer as? JavaStackFrame)?.descriptor
            is XValueGroupNodeImpl -> (node.valueContainer as? JavaStaticGroup)?.descriptor
            is WatchesRootNode -> null
            is WatchNodeImpl -> WatchItemDescriptor(project, TextWithImportsImpl(CodeFragmentKind.EXPRESSION, node.expression.expression))
            is MessageTreeNode -> MessageDescriptor(node.text.toString())
            else -> MessageDescriptor(node.toString())
        }

        if (descriptor != null && printDescriptor(node, descriptor, indent)) {
            return
        }

        printChildren(node, indent + 2)
    }

    fun printDescriptor(node: TreeNode, descriptor: NodeDescriptorImpl, indent: Int): Boolean {
        if (descriptor is DefaultNodeDescriptor || config.variablesToSkipInPrintFrame.contains(descriptor.name)) {
            return true
        }

        val label = calculateLabel(node, descriptor) ?: return true

        val project = delegate.debuggerContext.project
        val debugProcess = delegate.debuggerContext.debugProcess ?: error("Debugger process is not launched")

        val text = buildString {
            append(" ".repeat(indent + 1))
            append(getPrefix(descriptor))
            append(label)
            if (config.shouldRenderSourcesPosition() && hasSourcePosition(descriptor)) {
                val sp = debugProcess.invokeInManagerThread {
                    SourcePositionProvider.getSourcePosition(descriptor, project, delegate.debuggerContext)
                }
                append(" (sp = ${render(sp)})")
            }

            if (config.shouldRenderExpression() && descriptor is ValueDescriptorImpl) {
                val expression = debugProcess.invokeInManagerThread {
                    descriptor.getTreeEvaluation((node as XValueNodeImpl).valueContainer as JavaValue, it) as? PsiExpression
                }

                if (expression != null) {
                    val text = TextWithImportsImpl(expression)
                    val imports = expression.getUserData(DebuggerTreeNodeExpression.ADDITIONAL_IMPORTS_KEY)?.joinToString { it } ?: ""

                    val codeFragment = KotlinCodeFragmentFactory().createPresentationCodeFragment(
                        TextWithImportsImpl(text.kind, text.text, text.imports + imports, text.fileType),
                        delegate.debuggerContext.sourcePosition.elementAt, project
                    )
                    val codeFragmentText = codeFragment.text

                    if (config.shouldComputeResultOfCreateExpression()) {
                        debugProcess.invokeInManagerThread {
                            val suspendContext = it.suspendContext ?: error(SuspendContext::class.java.simpleName + " is not set")
                            val fragment = TextWithImportsImpl(text.kind, codeFragmentText, codeFragment.importsToString(), text.fileType)
                            delegate.evaluate(suspendContext, fragment)
                        }
                    }

                    append(" (expression = $codeFragmentText)")
                }
            }
            append("\n")
        }

        delegate.logDescriptor(descriptor, text)

        return false
    }

    private fun calculateLabel(node: TreeNode, descriptor: NodeDescriptorImpl): String? {
        var label = config.renderLabel(node, descriptor)

        // TODO: update presentation before calc label
        if (label == NodeDescriptorImpl.UNKNOWN_VALUE_MESSAGE && descriptor is StaticDescriptor) {
            label = "static = " + NodeRendererSettings.getInstance().classRenderer.renderTypeName(descriptor.type.name())
        }

        if (label.endsWith(XDebuggerUIConstants.COLLECTING_DATA_MESSAGE)) {
            return null
        }

        return label
    }

    private fun getPrefix(descriptor: NodeDescriptorImpl): String {
        val prefix = when (descriptor) {
            is StackFrameDescriptor -> "frame"
            is WatchItemDescriptor -> "extra"
            is LocalVariableDescriptor -> "local"
            is StaticDescriptor -> "static"
            is ThisDescriptorImpl -> "this"
            is FieldDescriptor -> "field"
            is ArrayElementDescriptor -> "element"
            is ContinuationVariableValueDescriptorImpl -> "cont"
            is MessageDescriptor -> ""
            else -> "unknown"
        }
        return prefix + " ".repeat("unknown ".length - prefix.length) + if (descriptor is MessageDescriptor) " - " else " = "
    }

    private fun hasSourcePosition(descriptor: NodeDescriptorImpl): Boolean {
        return when (descriptor) {
            is LocalVariableDescriptor,
            is FieldDescriptor -> true
            else -> false
        }
    }

    private fun printChildren(node: TreeNode, indent: Int) {
        val e = node.children()
        while (e.hasMoreElements()) {
            printNode(e.nextElement() as TreeNode, indent)
        }
    }

    private fun render(sp: SourcePosition?): String {
        return renderSourcePosition(sp).replace(":", ", ")
    }
}

fun renderSourcePosition(sourcePosition: SourcePosition?): String {
    if (sourcePosition == null) {
        return "null"
    }

    val virtualFile = sourcePosition.file.originalFile.virtualFile ?: sourcePosition.file.viewProvider.virtualFile

    val libraryEntry = LibraryUtil.findLibraryEntry(virtualFile, sourcePosition.file.project)
    if (libraryEntry != null && (libraryEntry is JdkOrderEntry || libraryEntry.presentableName == KOTLIN_LIBRARY_NAME)) {
        val suffix = if (sourcePosition.isInCompiledFile()) "COMPILED" else "EXT"
        return FileUtil.getNameWithoutExtension(virtualFile.name) + ".!$suffix!"
    }

    return virtualFile.name + ":" + (sourcePosition.line + 1)
}

private fun SourcePosition.isInCompiledFile(): Boolean {
    val ktFile = file as? KtFile ?: return false
    return ktFile.isCompiled
}