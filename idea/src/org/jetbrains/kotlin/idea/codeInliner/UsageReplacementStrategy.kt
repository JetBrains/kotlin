/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.codeInliner

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.ui.GuiUtils
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.targetDescriptors
import org.jetbrains.kotlin.idea.intentions.ConvertReferenceToLambdaIntention
import org.jetbrains.kotlin.idea.intentions.SpecifyExplicitLambdaSignatureIntention
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

interface UsageReplacementStrategy {
    fun createReplacer(usage: KtSimpleNameExpression): (() -> KtElement?)?
}

private val LOG = Logger.getInstance(UsageReplacementStrategy::class.java)

fun UsageReplacementStrategy.replaceUsagesInWholeProject(
    targetPsiElement: PsiElement,
    progressTitle: String,
    commandName: String,
    postAction: () -> Unit = {}
) {
    val project = targetPsiElement.project
    ProgressManager.getInstance().run(
        object : Task.Modal(project, progressTitle, true) {
            override fun run(indicator: ProgressIndicator) {
                val usages = runReadAction {
                    val searchScope = KotlinSourceFilterScope.projectSources(GlobalSearchScope.projectScope(project), project)
                    ReferencesSearch.search(targetPsiElement, searchScope)
                        .filterIsInstance<KtSimpleNameReference>()
                        .map { ref -> ref.expression }
                }
                this@replaceUsagesInWholeProject.replaceUsages(usages, targetPsiElement, project, commandName, postAction)
            }
        })
}

fun UsageReplacementStrategy.replaceUsages(
    usages: Collection<KtSimpleNameExpression>,
    targetPsiElement: PsiElement,
    project: Project,
    commandName: String,
    postAction: () -> Unit = {}
) {
    GuiUtils.invokeLaterIfNeeded({
                                     project.executeWriteCommand(commandName) {
                                         val targetDeclaration = targetPsiElement as? KtNamedDeclaration

                                         val usagesByFile = usages.groupBy { it.containingFile }

                                         val KEY = Key<Unit>("UsageReplacementStrategy.replaceUsages")

                                         for ((file, usagesInFile) in usagesByFile) {
                                             usagesInFile.forEach { it.putCopyableUserData(KEY, Unit) }

                                             // we should delete imports later to not affect other usages
                                             val importsToDelete = mutableListOf<KtImportDirective>()

                                             var usagesToProcess = usagesInFile
                                             while (usagesToProcess.isNotEmpty()) {
                                                 if (processUsages(usagesToProcess, targetDeclaration, importsToDelete)) break

                                                 // some usages may get invalidated we need to find them in the tree
                                                 usagesToProcess = file.collectDescendantsOfType { it.getCopyableUserData(KEY) != null }
                                             }

                                             file.forEachDescendantOfType<KtSimpleNameExpression> { it.putCopyableUserData(KEY, null) }

                                             importsToDelete.forEach { it.delete() }
                                         }

                                         postAction()
                                     }
                                 }, ModalityState.NON_MODAL)
}

/**
 * @return false if some usages were invalidated
 */
private fun UsageReplacementStrategy.processUsages(
    usages: List<KtSimpleNameExpression>,
    targetDeclaration: KtNamedDeclaration?,
    importsToDelete: MutableList<KtImportDirective>
): Boolean {
    var invalidUsagesFound = false
    for (usage in usages) {
        try {
            if (!usage.isValid) {
                invalidUsagesFound = true
                continue
            }

            if (specialUsageProcessing(usage, targetDeclaration)) continue

            //TODO: keep the import if we don't know how to replace some of the usages
            val importDirective = usage.getStrictParentOfType<KtImportDirective>()
            if (importDirective != null) {
                if (!importDirective.isAllUnder && importDirective.targetDescriptors().size == 1) {
                    importsToDelete.add(importDirective)
                }
                continue
            }

            createReplacer(usage)?.invoke()
        } catch (e: Throwable) {
            LOG.error(e)
        }
    }
    return !invalidUsagesFound
}

private fun UsageReplacementStrategy.specialUsageProcessing(
    usage: KtSimpleNameExpression,
    targetDeclaration: KtNamedDeclaration?
): Boolean {
    when (val usageParent = usage.parent) {
        is KtCallableReferenceExpression -> {
            val grandParent = usageParent.parent
            ConvertReferenceToLambdaIntention().applyTo(usageParent, null)
            (grandParent as? KtElement)?.let {
                doRefactoringInside(it, targetDeclaration?.name, targetDeclaration?.descriptor)
            }
            return true
        }

        is KtCallElement -> {
            val lambdaArguments = usageParent.lambdaArguments
            if (lambdaArguments.isNotEmpty()) {
                val grandParent = usageParent.parent
                val specifySignature = SpecifyExplicitLambdaSignatureIntention()
                for (lambdaArgument in lambdaArguments) {
                    val lambdaExpression = lambdaArgument.getLambdaExpression() ?: continue
                    val functionDescriptor =
                        lambdaExpression.functionLiteral.resolveToDescriptorIfAny() as? FunctionDescriptor ?: continue
                    if (functionDescriptor.valueParameters.isNotEmpty()) {
                        specifySignature.applyTo(lambdaExpression, null)
                    }
                }
                (grandParent as? KtElement)?.let {
                    doRefactoringInside(it, targetDeclaration?.name, targetDeclaration?.descriptor)
                }
                return true
            }
        }

    }
    return false
}

private fun UsageReplacementStrategy.doRefactoringInside(
    element: KtElement, targetName: String?, targetDescriptor: DeclarationDescriptor?
) {
    element.forEachDescendantOfType<KtSimpleNameExpression> { usage ->
        if (usage.isValid && usage.getReferencedName() == targetName) {
            if (targetDescriptor == usage.resolveToCall()?.candidateDescriptor) {
                createReplacer(usage)?.invoke()
            }
        }
    }
}
