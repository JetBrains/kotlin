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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diff.DiffManager
import com.intellij.openapi.diff.SimpleContent
import com.intellij.openapi.diff.SimpleDiffRequest
import com.intellij.openapi.diff.ex.DiffPanelOptions
import com.intellij.openapi.diff.impl.DiffPanelImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.ArrayList
import javax.swing.SwingUtilities

class CheckPartialBodyResolveAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val selectedFiles = selectedKotlinFiles(e).toList()
        val project = CommonDataKeys.PROJECT.getData(e.dataContext)!!

        ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    runReadAction { checkResolve(selectedFiles, project) }
                },
                "Checking Partial Body Resolve",
                true,
                project)
    }

    private fun checkResolve(files: Collection<KtFile>, project: Project) {
        //TODO: drop resolve caches if any
        val progressIndicator = ProgressManager.getInstance().progressIndicator
        for ((i, file) in files.withIndex()) {
            progressIndicator?.text = "Checking resolve $i of ${files.size}..."
            progressIndicator?.text2 = file.virtualFile.path

            val partialResolveDump = dumpResolve(file) { element, resolutionFacade ->
                resolutionFacade.analyze(element, BodyResolveMode.PARTIAL)
            }
            val goldDump = dumpResolve(file) { element, resolutionFacade ->
                resolutionFacade.analyze(element)
            }
            if (partialResolveDump != goldDump) {
                SwingUtilities.invokeLater {
                    val title = "Difference Found in File ${file.virtualFile.path}"

                    val request = SimpleDiffRequest(project, title)
                    request.setContents(SimpleContent(goldDump), SimpleContent(partialResolveDump))
                    request.setContentTitles("Expected", "Partial Body Resolve")
                    val diffBuilder = DialogBuilder(project)
                    val diffPanel = DiffManager.getInstance().createDiffPanel(diffBuilder.window, project, diffBuilder, null) as DiffPanelImpl
                    diffPanel.options.setShowSourcePolicy(DiffPanelOptions.ShowSourcePolicy.DONT_SHOW)
                    diffBuilder.setCenterPanel(diffPanel.component)
                    diffBuilder.setDimensionServiceKey("org.jetbrains.kotlin.idea.actions.internal.CheckPartialBodyResolveAction.Diff")
                    diffPanel.setDiffRequest(request)
                    diffBuilder.addOkAction().setText("Close")
                    diffBuilder.setTitle(title)
                    diffBuilder.showNotModal()
                    //TODO: choose continue or abort
                }
                return
            }

            progressIndicator?.fraction = (i + 1) / files.size.toDouble()
        }

        SwingUtilities.invokeLater {
            Messages.showInfoMessage(project, "Analyzed ${files.size} file(s). No resolve difference found. ", "Success")
        }
    }

    private fun dumpResolve(file: KtFile, resolver: (KtElement, ResolutionFacade) -> BindingContext): String {
        val builder = StringBuilder()
        val resolutionFacade = file.getResolutionFacade()
        val document = FileDocumentManager.getInstance().getDocument(file.virtualFile)!!
        file.acceptChildren(object : KtVisitorVoid(){
            override fun visitKtElement(element: KtElement) {
                ProgressManager.checkCanceled()
                element.acceptChildren(this)
            }

            override fun visitExpression(expression: KtExpression) {
                super.visitExpression(expression)

                // do not try to resolve to declaration as it crashes on some declaration (namely JetClassInitializer)
                if (expression is KtDeclaration) return

                if (!isValueNeeded(expression)) return

                val bindingContext = resolver(expression, resolutionFacade)

                val offset = expression.textOffset
                val line = document.getLineNumber(offset)
                val column = offset - document.getLineStartOffset(line)
                val exprName = (expression as? KtNameReferenceExpression)?.getReferencedName() ?: expression::class.java.simpleName
                builder.append("$exprName at (${line + 1}:${column + 1})")

                if (expression is KtReferenceExpression) {
                    val target = bindingContext[BindingContext.REFERENCE_TARGET, expression]
                    builder.append(" resolves to ${target?.presentation()}")
                }

                val type = bindingContext.getType(expression)
                if (type != null) {
                    builder.append(" has type ${type.presentation()}")
                }

                builder.append("\n")
            }
        })
        return builder.toString()
    }

    private fun DeclarationDescriptor.presentation() = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(this)
    private fun KotlinType.presentation() = DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(this)

    override fun update(e: AnActionEvent) {
        if (!KotlinInternalMode.enabled) {
            e.presentation.isVisible = false
            e.presentation.isEnabled = false
        }
        else {
            e.presentation.isVisible = true
            e.presentation.isEnabled = selectedKotlinFiles(e).any()
        }
    }

    private fun selectedKotlinFiles(e: AnActionEvent): Sequence<KtFile> {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return sequenceOf()
        val project = CommonDataKeys.PROJECT.getData(e.dataContext) ?: return sequenceOf()
        return allKotlinFiles(virtualFiles, project)
    }

    private fun allKotlinFiles(filesOrDirs: Array<VirtualFile>, project: Project): Sequence<KtFile> {
        val manager = PsiManager.getInstance(project)
        return allFiles(filesOrDirs)
                .asSequence()
                .mapNotNull { manager.findFile(it) as? KtFile }
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
    private fun isValueNeeded(expression: KtExpression): Boolean {
        val parent = expression.parent
        return when (parent) {
            is KtBlockExpression -> expression == parent.lastStatement() && isValueNeeded(parent)

            is KtContainerNode -> { //TODO - not quite correct
                val pparent = parent.parent as? KtExpression
                pparent != null && isValueNeeded(pparent)
            }

            is KtDeclarationWithBody -> {
                if (expression == parent.bodyExpression)
                    !parent.hasBlockBody() && !parent.hasDeclaredReturnType()
                else
                    true
            }

            else -> true
        }
    }

    private fun KtBlockExpression.lastStatement(): KtExpression?
            = lastChild?.siblings(forward = false)?.firstIsInstanceOrNull<KtExpression>()
}
