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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiManager
import com.intellij.usageView.UsageInfo
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.usages.UsageTarget
import com.intellij.usages.UsageViewManager
import com.intellij.usages.UsageViewPresentation
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.types.KotlinType
import java.util.*
import javax.swing.SwingUtilities

class FindImplicitNothingAction : AnAction() {
    private val LOG = Logger.getInstance("#org.jetbrains.kotlin.idea.actions.internal.FindImplicitNothingAction")

    override fun actionPerformed(e: AnActionEvent) {
        val selectedFiles = selectedKotlinFiles(e).toList()
        val project = CommonDataKeys.PROJECT.getData(e.dataContext)!!

        ProgressManager.getInstance().runProcessWithProgressSynchronously(
                Runnable { find(selectedFiles, project) },
                "Finding Implicit Nothing's",
                true,
                project)
    }

    private fun find(files: Collection<KtFile>, project: Project) {
        val progressIndicator = ProgressManager.getInstance().progressIndicator
        val found = ArrayList<KtCallExpression>()
        for ((i, file) in files.withIndex()) {
            progressIndicator?.text = "Scanning files: $i of ${files.size} file. ${found.size} occurences found"
            progressIndicator?.text2 = file.virtualFile.path

            val resolutionFacade = file.getResolutionFacade()
            file.acceptChildren(object : KtVisitorVoid() {
                override fun visitKtElement(element: KtElement) {
                    ProgressManager.checkCanceled()
                    element.acceptChildren(this)
                }

                override fun visitCallExpression(expression: KtCallExpression) {
                    expression.acceptChildren(this)

                    try {
                        val bindingContext = resolutionFacade.analyze(expression)
                        val type = bindingContext.getType(expression) ?: return
                        if (KotlinBuiltIns.isNothing(type) && !expression.hasExplicitNothing(bindingContext)) { //TODO: what about nullable Nothing?
                            found.add(expression)
                        }
                    }
                    catch(e: ProcessCanceledException) {
                        throw e
                    }
                    catch(t: Throwable) { // do not stop on internal error
                        LOG.error(t)
                    }
                }
            })

            progressIndicator?.fraction = (i + 1) / files.size.toDouble()
        }

        SwingUtilities.invokeLater {
            if (found.isNotEmpty()) {
                val usages = found.map { UsageInfo2UsageAdapter(UsageInfo(it)) }.toTypedArray()
                val presentation = UsageViewPresentation()
                presentation.tabName = "Implicit Nothing's"
                UsageViewManager.getInstance(project).showUsages(arrayOf<UsageTarget>(), usages, presentation)
            }
            else {
                Messages.showInfoMessage(project, "Not found in ${files.size} file(s)", "Not Found")
            }
        }
    }

    private fun KtExpression.hasExplicitNothing(bindingContext: BindingContext): Boolean {
        val callee = getCalleeExpressionIfAny() ?: return false
        when (callee) {
            is KtSimpleNameExpression -> {
                val target = bindingContext[BindingContext.REFERENCE_TARGET, callee] ?: return false
                val callableDescriptor = (target as? CallableDescriptor ?: return false).original
                val declaration = DescriptorToSourceUtils.descriptorToDeclaration(callableDescriptor) as? KtCallableDeclaration
                if (declaration != null && declaration.typeReference == null) return false // implicit type
                val type = callableDescriptor.returnType ?: return false
                return type.isNothingOrNothingFunctionType()
            }

            else -> {
                return callee.hasExplicitNothing(bindingContext)
            }
        }
    }

    private fun KotlinType.isNothingOrNothingFunctionType(): Boolean {
        return KotlinBuiltIns.isNothing(this) ||
               (isFunctionType && this.getReturnTypeFromFunctionType().isNothingOrNothingFunctionType())
    }

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
}
