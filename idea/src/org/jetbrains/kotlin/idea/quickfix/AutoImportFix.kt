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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.daemon.impl.ShowAutoImportPass
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.actions.JetAddImportAction
import org.jetbrains.kotlin.idea.caches.JetShortNamesCache
import org.jetbrains.kotlin.idea.caches.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import java.util.ArrayList
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.diagnostics.Errors
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.idea.completion.isVisible
import org.jetbrains.kotlin.utils.CachedValueProperty
import org.jetbrains.kotlin.idea.util.psiClassToDescriptor.psiClassToDescriptor

/**
 * Check possibility and perform fix for unresolved references.
 */
public class AutoImportFix(element: JetSimpleNameExpression) : JetHintAction<JetSimpleNameExpression>(element), HighPriorityAction {
    private val modificationCountOnCreate = PsiModificationTracker.SERVICE.getInstance(element.getProject()).getModificationCount()

    volatile private var anySuggestionFound: Boolean? = null

    private val suggestions: Collection<DeclarationDescriptor> by CachedValueProperty(
            {
                val descriptors = computeSuggestions(element)
                anySuggestionFound = !descriptors.isEmpty()
                descriptors
            },
            { PsiModificationTracker.SERVICE.getInstance(element.getProject()).getModificationCount() })

    override fun showHint(editor: Editor): Boolean {
        if (!element.isValid() || isOutdated()) return false

        if (HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)) return false

        if (suggestions.isEmpty()) return false

        if (!ApplicationManager.getApplication()!!.isUnitTestMode()) {
            val hintText = ShowAutoImportPass.getMessage(suggestions.size() > 1, DescriptorUtils.getFqNameSafe(suggestions.first()).asString())

            val project = editor.getProject() ?: return false
            HintManager.getInstance().showQuestionHint(editor, hintText, element.getTextOffset(), element.getTextRange()!!.getEndOffset(), createAction(project, editor))
        }

        return true
    }

    override fun getText() = JetBundle.message("import.fix")

    override fun getFamilyName() = JetBundle.message("import.fix")

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile)
            = (super<JetHintAction>.isAvailable(project, editor, file)) && (anySuggestionFound ?: !suggestions.isEmpty())

    override fun invoke(project: Project, editor: Editor?, file: JetFile?) {
        CommandProcessor.getInstance().runUndoTransparentAction {
            createAction(project, editor!!).execute()
        }
    }

    override fun startInWriteAction() = true

    private fun isOutdated() = modificationCountOnCreate != PsiModificationTracker.SERVICE.getInstance(element.getProject()).getModificationCount()

    private fun createAction(project: Project, editor: Editor) = JetAddImportAction(project, editor, element, suggestions)

    private fun computeSuggestions(element: JetSimpleNameExpression): Collection<DeclarationDescriptor> {
        if (!element.isValid()) return listOf()

        val file = element.getContainingFile() as? JetFile ?: return listOf()

        var referenceName = element.getReferencedName()
        if (element.getIdentifier() == null) {
            val conventionName = JetPsiUtil.getConventionName(element)
            if (conventionName != null) {
                referenceName = conventionName.asString()
            }
        }
        if (referenceName.isEmpty()) return listOf()

        val resolutionFacade = element.getResolutionFacade()

        val searchScope = file.getResolveScope()

        val bindingContext = resolutionFacade.analyze(element)

        val diagnostics = bindingContext.getDiagnostics().forElement(element)
        if (!diagnostics.any { it.getFactory() in ERRORS }) return listOf()

        val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, element] ?: return listOf()
        val containingDescriptor = resolutionScope.getContainingDeclaration()

        fun isVisible(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor is DeclarationDescriptorWithVisibility) {
                return descriptor.isVisible(containingDescriptor, bindingContext, element)
            }

            return true
        }

        val result = ArrayList<DeclarationDescriptor>()

        val moduleDescriptor = resolutionFacade.findModuleDescriptor(element)
        val indicesHelper = KotlinIndicesHelper(file.getProject(), resolutionFacade, bindingContext, searchScope, moduleDescriptor, ::isVisible)

        if (!element.isImportDirectiveExpression() && !JetPsiUtil.isSelectorInQualified(element)) {
            getClasses(referenceName, file, searchScope).filterTo(result, ::isVisible)
            result.addAll(indicesHelper.getTopLevelCallablesByName(referenceName))
        }

        result.addAll(indicesHelper.getCallableExtensions({ it == referenceName }, element))

        return result
    }

    private fun getClasses(name: String, file: JetFile, searchScope: GlobalSearchScope): Collection<DeclarationDescriptor>
            = getShortNamesCache(file).getClassesByName(name, searchScope)
            .map { element.getResolutionFacade().psiClassToDescriptor(it) }
            .filterNotNull()
            .toSet()

    private fun getShortNamesCache(jetFile: JetFile): PsiShortNamesCache {
        // if we are in JS module, do not include non-kotlin classes
        return if (ProjectStructureUtil.isJsKotlinModule(jetFile))
            JetShortNamesCache.getKotlinInstance(jetFile.getProject())
        else
            PsiShortNamesCache.getInstance(jetFile.getProject())
    }

    class object {
        private val ERRORS = setOf(Errors.UNRESOLVED_REFERENCE, Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER)

        public fun createFactory(): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): JetIntentionAction<JetSimpleNameExpression>? {
                    // There could be different psi elements (i.e. JetArrayAccessExpression), but we can fix only JetSimpleNameExpression case
                    val psiElement = diagnostic.getPsiElement()
                    if (psiElement is JetSimpleNameExpression) {
                        return AutoImportFix(psiElement)
                    }

                    return null
                }

                override fun isApplicableForCodeFragment()
                        = true
            }
        }
    }
}
