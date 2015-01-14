/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.actions.internal

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import java.util.ArrayList
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.kotlin.psi.JetVisitorVoid
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import com.intellij.openapi.diff.DiffManager
import com.intellij.openapi.diff.SimpleDiffRequest
import com.intellij.openapi.diff.SimpleContent
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.diff.impl.DiffPanelImpl
import com.intellij.openapi.diff.ex.DiffPanelOptions
import com.intellij.openapi.ui.Messages
import javax.swing.SwingUtilities
import org.jetbrains.kotlin.psi.JetNameReferenceExpression
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.psi.JetBlockExpression
import org.jetbrains.kotlin.psi.JetDeclarationWithBody
import org.jetbrains.kotlin.psi.JetContainerNode
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

public class CheckPartialBodyResolveAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val selectedFiles = selectedKotlinFiles(e).toList()
        val project = CommonDataKeys.PROJECT.getData(e.getDataContext())!!

        ProgressManager.getInstance().runProcessWithProgressSynchronously(
                Runnable { checkResolve(selectedFiles, project) },
                "Checking Partial Body Resolve",
                true,
                project)
    }

    private fun checkResolve(files: Collection<JetFile>, project: Project) {
        //TODO: drop resolve caches if any
        val progressIndicator = ProgressManager.getInstance().getProgressIndicator()
        for ((i, file) in files.withIndices()) {
            progressIndicator?.setText("Checking resolve $i of ${files.size}...")
            progressIndicator?.setText2(file.getVirtualFile().getPath())

            val partialResolveDump = dumpResolve(file) {(element, resolutionFacade) ->
                resolutionFacade.analyze(element, BodyResolveMode.PARTIAL)
            }
            val goldDump = dumpResolve(file) {(element, resolutionFacade) ->
                resolutionFacade.analyze(element)
            }
            if (partialResolveDump != goldDump) {
                SwingUtilities.invokeLater {
                    val title = "Difference Found in File ${file.getVirtualFile().getPath()}"

                    val request = SimpleDiffRequest(project, title)
                    request.setContents(SimpleContent(goldDump), SimpleContent(partialResolveDump))
                    request.setContentTitles("Expected", "Partial Body Resolve")
                    val diffBuilder = DialogBuilder(project)
                    val diffPanel = DiffManager.getInstance().createDiffPanel(diffBuilder.getWindow(), project, diffBuilder, null) as DiffPanelImpl
                    diffPanel.getOptions().setShowSourcePolicy(DiffPanelOptions.ShowSourcePolicy.DONT_SHOW)
                    diffBuilder.setCenterPanel(diffPanel.getComponent())
                    diffBuilder.setDimensionServiceKey("org.jetbrains.kotlin.idea.actions.internal.CheckPartialBodyResolveAction.Diff")
                    diffPanel.setDiffRequest(request)
                    diffBuilder.addOkAction().setText("Close")
                    diffBuilder.setTitle(title)
                    diffBuilder.showNotModal()
                    //TODO: choose continue or abort
                }
                return
            }

            progressIndicator?.setFraction((i + 1) / files.size.toDouble())
        }

        SwingUtilities.invokeLater {
            Messages.showInfoMessage(project, "Analyzed ${files.size} file(s). No resolve difference found. ", "Success")
        }
    }

    private fun dumpResolve(file: JetFile, resolver: (JetElement, ResolutionFacade) -> BindingContext): String {
        val builder = StringBuilder()
        val resolutionFacade = file.getResolutionFacade()
        val document = FileDocumentManager.getInstance().getDocument(file.getVirtualFile())
        file.acceptChildren(object : JetVisitorVoid(){
            override fun visitJetElement(element: JetElement) {
                ProgressManager.checkCanceled()
                element.acceptChildren(this)
            }

            override fun visitExpression(expression: JetExpression) {
                super.visitExpression(expression)

                // do not try to resolve to declaration as it crashes on some declaration (namely JetClassInitializer)
                if (expression is JetDeclaration) return

                if (!isValueNeeded(expression)) return

                val bindingContext = resolver(expression, resolutionFacade)

                val offset = expression.getTextOffset()
                val line = document.getLineNumber(offset)
                val column = offset - document.getLineStartOffset(line)
                val exprName = if (expression is JetNameReferenceExpression) expression.getReferencedName() else expression.javaClass.getSimpleName()
                builder.append("$exprName at (${line + 1}:${column + 1})")

                if (expression is JetReferenceExpression) {
                    val target = bindingContext[BindingContext.REFERENCE_TARGET, expression]
                    builder.append(" resolves to ${target?.presentation()}")
                }

                val type = bindingContext[BindingContext.EXPRESSION_TYPE, expression]
                if (type != null) {
                    builder.append(" has type ${type.presentation()}")
                }

                builder.append("\n")
            }
        })
        return builder.toString()
    }

    private fun DeclarationDescriptor.presentation() = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(this)
    private fun JetType.presentation() = DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(this)

    override fun update(e: AnActionEvent) {
        if (!KotlinInternalMode.enabled) {
            e.getPresentation().setVisible(false)
            e.getPresentation().setEnabled(false)
        }
        e.getPresentation().setVisible(true)
        e.getPresentation().setEnabled(selectedKotlinFiles(e).any())
    }

    private fun selectedKotlinFiles(e: AnActionEvent): Stream<JetFile> {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return streamOf()
        val project = CommonDataKeys.PROJECT.getData(e.getDataContext()) ?: return streamOf()
        return allKotlinFiles(virtualFiles, project)
    }

    private fun allKotlinFiles(filesOrDirs: Array<VirtualFile>, project: Project): Stream<JetFile> {
        val manager = PsiManager.getInstance(project)
        return allFiles(filesOrDirs)
                .stream()
                .map { manager.findFile(it) as? JetFile }
                .filterNotNull()
    }

    private fun allFiles(filesOrDirs: Array<VirtualFile>): Collection<VirtualFile> {
        val result = ArrayList<VirtualFile>()
        for (file in filesOrDirs) {
            VfsUtilCore.visitChildrenRecursively(file, object : VirtualFileVisitor<Unit>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    result.add(file)
                    return true
                }
            })
        }
        return result
    }

    //TODO: currently copied from PartialBodyResolveFilter - not good
    private fun isValueNeeded(expression: JetExpression): Boolean {
        val parent = expression.getParent()
        return when (parent) {
            is JetBlockExpression -> expression == parent.lastStatement() && isValueNeeded(parent)

            is JetContainerNode -> { //TODO - not quite correct
                val pparent = parent.getParent() as? JetExpression
                pparent != null && isValueNeeded(pparent)
            }

            is JetDeclarationWithBody -> {
                if (expression == parent.getBodyExpression())
                    !parent.hasBlockBody() && !parent.hasDeclaredReturnType()
                else
                    true
            }

            else -> true
        }
    }

    private fun JetBlockExpression.lastStatement(): JetExpression?
            = getLastChild()?.siblings(forward = false)?.firstIsInstanceOrNull<JetExpression>()
}
