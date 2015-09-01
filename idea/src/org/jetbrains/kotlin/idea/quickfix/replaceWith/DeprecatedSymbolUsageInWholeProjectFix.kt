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

package org.jetbrains.kotlin.idea.quickfix.replaceWith

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.targetDescriptors
import org.jetbrains.kotlin.idea.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.stubindex.JetSourceFilterScope
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.NameShortness
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy

public class DeprecatedSymbolUsageInWholeProjectFix(
        element: JetSimpleNameExpression,
        replaceWith: ReplaceWith,
        private val text: String
) : DeprecatedSymbolUsageFixBase(element, replaceWith) {

    private val LOG = Logger.getInstance(DeprecatedSymbolUsageInWholeProjectFix::class.java);

    override fun getFamilyName() = "Replace deprecated symbol usage in whole project"

    override fun getText() = text

    override fun startInWriteAction() = false

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        if (!super.isAvailable(project, editor, file)) return false
        val targetPsiElement = element.mainReference.resolve()
        return targetPsiElement is JetNamedFunction || targetPsiElement is JetProperty //TODO
    }

    override fun invoke(replacementStrategy: UsageReplacementStrategy, project: Project, editor: Editor?) {
        val psiElement = element.mainReference.resolve()!!

        ProgressManager.getInstance().run(
                object : Task.Modal(project, "Applying '$text'", true) {
                    override fun run(indicator: ProgressIndicator) {
                        val usages = runReadAction {
                            val searchScope = JetSourceFilterScope.kotlinSources(GlobalSearchScope.projectScope(project), project)
                            ReferencesSearch.search(psiElement, searchScope)
                                    .filterIsInstance<JetSimpleNameReference>()
                                    .map { ref -> ref.expression }
                        }
                        replaceUsages(project, usages, replacementStrategy)
                    }
                })
    }

    private fun replaceUsages(project: Project, usages: Collection<JetSimpleNameExpression>, replacementStrategy: UsageReplacementStrategy) {
        UIUtil.invokeLaterIfNeeded {
            project.executeWriteCommand(text) {
                // we should delete imports later to not affect other usages
                val importsToDelete = arrayListOf<JetImportDirective>()

                for (usage in usages) {
                    try {
                        if (!usage.isValid) continue // TODO: nested calls

                        //TODO: keep the import if we don't know how to replace some of the usages
                        val importDirective = usage.getStrictParentOfType<JetImportDirective>()
                        if (importDirective != null) {
                            if (!importDirective.isAllUnder && importDirective.targetDescriptors().size() == 1) {
                                importsToDelete.add(importDirective)
                            }
                            continue
                        }

                        replacementStrategy.createReplacer(usage)?.invoke()
                    }
                    catch (e: Throwable) {
                        LOG.error(e)
                    }
                }

                importsToDelete.forEach { it.delete() }
            }
        }
    }

    companion object : JetSingleIntentionActionFactory() {
        //TODO: better rendering needed
        private val RENDERER = DescriptorRenderer.withOptions {
            modifiers = emptySet()
            nameShortness = NameShortness.SHORT
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
            receiverAfterName = true
            renderCompanionObjectName = true
            withoutSuperTypes = true
            startFromName = true
            withDefinedIn = false
        }

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val nameExpression = diagnostic.psiElement as? JetSimpleNameExpression ?: return null
            val descriptor = Errors.DEPRECATED_SYMBOL_WITH_MESSAGE.cast(diagnostic).a
            val replacement = DeprecatedSymbolUsageFixBase.replaceWithPattern(descriptor, nameExpression.project) ?: return null
            val descriptorName = RENDERER.render(descriptor)
            return DeprecatedSymbolUsageInWholeProjectFix(nameExpression, replacement, "Replace usages of '$descriptorName' in whole project")
        }

    }
}
