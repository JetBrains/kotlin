/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.IndexComparator
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.SimpleNode
import com.intellij.ui.treeStructure.SimpleTreeBuilder
import com.intellij.ui.treeStructure.SimpleTreeStructure
import com.intellij.ui.treeStructure.Tree
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSessionBase
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import kotlin.math.min
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

class FirExplorerToolWindow(private val project: Project, private val toolWindow: ToolWindow) : JPanel(BorderLayout()), Disposable {


    private val tree = Tree()
    private val treeStructure = FirExplorerTreeStructure()
    private val builder = SimpleTreeBuilder(tree, DefaultTreeModel(DefaultMutableTreeNode()), treeStructure, IndexComparator.INSTANCE)

    private val level = HighlighterLayer.SELECTION - 100
    private val highlightingAttributes = TextAttributes().apply {
        effectType = EffectType.ROUNDED_BOX
        effectColor = JBColor.RED
    }


    private val rangeHighlightMarkers = mutableListOf<RangeHighlighter>()

    private var currentEditor: Editor? = FileEditorManager.getInstance(project).selectedTextEditor

    fun clearHighlighting(editor: Editor) {
        rangeHighlightMarkers.forEach { editor.markupModel.removeHighlighter(it) }
        rangeHighlightMarkers.clear()
    }

    fun postponeTreeRebuild(editor: Editor, init: Boolean) {
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(
            object : Task.Backgroundable(project, "") {
                override fun run(indicator: ProgressIndicator) {
                    val psiDocumentManager = PsiDocumentManager.getInstance(project)
                    val file = runReadAction { psiDocumentManager.getPsiFile(editor.document) as? KtFile }
                    if (file != null) {
                        val firFile = runReadAction { RawFirBuilder(object : FirSessionBase() {}, stubMode = false).buildFirFile(file) }
                        runInEdt {
                            treeStructure.root = FirExplorerTreeNode("root = ", firFile, null)
                            builder.updateFromRoot(!init)
                        }
                    }
                }
            },
            EmptyProgressIndicator()
        )

    }

    init {


        project.messageBus.connect(this).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    runInEdt {
                        val oldEditor = (event.oldEditor as? TextEditor)
                        val textEditor = (event.newEditor as? TextEditor)
                        if (oldEditor != null && oldEditor.editor == currentEditor) {
                            clearHighlighting(currentEditor!!)
                            currentEditor = null
                        }

                        if (textEditor != null) {
                            currentEditor = textEditor.editor
                            postponeTreeRebuild(currentEditor!!, false)
                        }
                    }
                }
            }
        )

        tree.addTreeSelectionListener { event ->
            val currentEditor = FileEditorManager.getInstance(project).selectedTextEditor
            if (currentEditor != null) {
                runReadAction {
                    clearHighlighting(currentEditor)
                    event.paths.filter { event.isAddedPath(it) }.mapNotNull {
                        val treeNode = it.lastPathComponent
                        if (treeNode is DefaultMutableTreeNode) {
                            val userObject = treeNode.userObject
                            if (userObject is FirExplorerTreeNode) {

                                val data = userObject.data
                                val psi = (data as? FirElement)?.psi
                                if (data is FirElement && psi != null) {
                                    if (FileDocumentManager.getInstance().getFile(currentEditor.document) == psi.containingFile.virtualFile) {

                                        rangeHighlightMarkers += currentEditor.markupModel.addRangeHighlighter(
                                            psi.startOffset,
                                            min(currentEditor.document.textLength, psi.endOffset),
                                            level,
                                            highlightingAttributes,
                                            HighlighterTargetArea.EXACT_RANGE
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }






        add(tree)
        builder.initRoot()
        postponeTreeRebuild(currentEditor!!, true)
    }

    abstract class AbstractFirExplorerTreeNode(val propertyName: String, parent: SimpleNode?) : SimpleNode(parent) {

        init {
            templatePresentation.addText(propertyName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }

    }

    class FirExplorerTreeNode(propertyName: String, val data: Any?, parent: SimpleNode?) :
        AbstractFirExplorerTreeNode(propertyName, parent) {

        init {
            templatePresentation.setIcon(AllIcons.Nodes.Property)
            if (data != null) {
                templatePresentation.addText(data::class.java.simpleName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                templatePresentation.addText("(toString = $data)", SimpleTextAttributes.REGULAR_ATTRIBUTES)
            } else {
                templatePresentation.addText("null", SimpleTextAttributes.GRAY_ATTRIBUTES)
            }
        }


        private fun KClass<*>.getMemberProperties(): Collection<KProperty<*>> {
            return try {
                memberProperties
            } catch (e: Throwable) {
                try {
                    declaredMemberProperties
                } catch (e: Throwable) {
                    emptyList()
                }
            }
        }

        override fun getChildren(): Array<SimpleNode> {
            if (data == null) {
                return SimpleNode.NO_CHILDREN
            } else {
                val classOfData = data::class

                val members = classOfData.getMemberProperties()

                return members.filter {
                    it.visibility == null || it.visibility!! <= KVisibility.PROTECTED
                }.filter {
                    it.instanceParameter != null
                }.map {
                    it.getter.isAccessible = true
                    val value = it.getter.call(data)
                    val notNullValueType = it.returnType.withNullability(false)

                    val namePrefix = "${it.name}: ${it.returnType.renderSimple()} = "

                    when {
                        notNullValueType.isSubtypeOf(firElementType) -> {
                            FirExplorerTreeNode(namePrefix, value as FirElement?, this)
                        }
                        notNullValueType.isSubtypeOf(listElementType) -> {
                            FirExplorerTreeListNode(namePrefix, value as List<*>?, this)
                        }
                        else -> {
                            FirExplorerTreeNode(namePrefix, value, this)
                        }
                    }
                }.toTypedArray()
            }
        }

        companion object {
            val firElementKlass = FirElement::class
            val firElementType = firElementKlass.starProjectedType
            val listElementType = List::class.starProjectedType
        }
    }


    class FirExplorerTreeListNode(propertyName: String, val data: List<Any?>?, parent: SimpleNode?) :
        AbstractFirExplorerTreeNode(propertyName, parent) {

        init {
            if (data != null) {
                templatePresentation.addText(data::class.simpleName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                templatePresentation.addText("(size = ${data.size})", SimpleTextAttributes.REGULAR_ATTRIBUTES)
            } else {
                templatePresentation.addText("null", SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }
        }

        override fun update(presentation: PresentationData) {
            super.update(presentation)
            presentation.setIcon(AllIcons.General.Recursive)
        }

        override fun getChildren(): Array<SimpleNode> {
            if (data == null) return SimpleNode.NO_CHILDREN


            return data.mapIndexed { index, any ->
                val propName = "[$index] = "
                if (any is List<*>) {
                    FirExplorerTreeListNode(propName, any, this)
                } else {
                    FirExplorerTreeNode(propName, any, this)
                }
            }.toTypedArray()

        }

    }


    inner class FirExplorerTreeStructure : SimpleTreeStructure() {
        var root: SimpleNode = FirExplorerTreeListNode("root = ", null, null)

        override fun getRootElement(): Any {
            return root
        }
    }

    override fun dispose() {

    }

}

private fun KType.renderSimple(): String {
    fun KVariance?.render(): String {
        return when (this) {
            KVariance.IN -> "in "
            KVariance.OUT -> "out "
            else -> ""
        }
    }

    return buildString {
        val classifier = classifier
        when (classifier) {
            is KClass<*> -> append(classifier.simpleName)
            is KTypeParameter -> append(classifier.name)
        }
        if (arguments.isNotEmpty()) {
            append("<")
            arguments.joinTo(this) {
                it.variance.render() + it.type?.renderSimple()
            }
            append(">")
        }
        if (isMarkedNullable) {
            append("?")
        }
    }
}