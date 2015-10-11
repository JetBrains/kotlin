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
import com.intellij.packageDependencies.DependencyValidationManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.actions.KotlinAddImportAction
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.getResolveScope
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.core.getResolutionScope
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.isImportDirectiveExpression
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.CachedValueProperty
import java.util.*

/**
 * Check possibility and perform fix for unresolved references.
 */
public class AutoImportFix(element: JetSimpleNameExpression) : JetHintAction<JetSimpleNameExpression>(element), HighPriorityAction {
    private val modificationCountOnCreate = PsiModificationTracker.SERVICE.getInstance(element.getProject()).getModificationCount()

    @Volatile private var anySuggestionFound: Boolean? = null

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
            val addImportAction = createAction(element.project, editor)
            val hintText = ShowAutoImportPass.getMessage(suggestions.size() > 1, addImportAction.highestPriorityFqName.asString())
            HintManager.getInstance().showQuestionHint(editor, hintText, element.getTextOffset(), element.getTextRange()!!.getEndOffset(), addImportAction)
        }

        return true
    }

    override fun getText() = JetBundle.message("import.fix")

    override fun getFamilyName() = JetBundle.message("import.fix")

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile)
            = (super.isAvailable(project, editor, file)) && (anySuggestionFound ?: !suggestions.isEmpty())

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        CommandProcessor.getInstance().runUndoTransparentAction {
            createAction(project, editor!!).execute()
        }
    }

    override fun startInWriteAction() = true

    private fun isOutdated() = modificationCountOnCreate != PsiModificationTracker.SERVICE.getInstance(element.getProject()).getModificationCount()

    private fun createAction(project: Project, editor: Editor) = KotlinAddImportAction(project, editor, element, suggestions)

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): JetIntentionAction<JetSimpleNameExpression>? {
            // There could be different psi elements (i.e. JetArrayAccessExpression), but we can fix only JetSimpleNameExpression case
            val psiElement = diagnostic.getPsiElement()
            if (psiElement is JetSimpleNameExpression) {
                return AutoImportFix(psiElement)
            }

            return null
        }

        override fun isApplicableForCodeFragment() = true

        private val ERRORS by lazy(LazyThreadSafetyMode.PUBLICATION ) { QuickFixes.getInstance().getDiagnostics(this) }

        public fun computeSuggestions(element: JetSimpleNameExpression): Collection<DeclarationDescriptor> {
            if (!element.isValid()) return emptyList()

            val file = element.getContainingFile() as? JetFile ?: return emptyList()

            val callTypeAndReceiver = CallTypeAndReceiver.detect(element)
            if (callTypeAndReceiver is CallTypeAndReceiver.UNKNOWN) return emptyList()

            fun filterByCallType(descriptor: DeclarationDescriptor)
                    = callTypeAndReceiver.callType.descriptorKindFilter.accepts(descriptor)

            var referenceName = element.getReferencedName()
            if (element.getIdentifier() == null) {
                val conventionName = JetPsiUtil.getConventionName(element)
                if (conventionName != null) {
                    referenceName = conventionName.asString()
                }
            }
            if (referenceName.isEmpty()) return emptyList()

            val searchScope = getResolveScope(file)

            val bindingContext = element.analyze(BodyResolveMode.PARTIAL)

            val diagnostics = bindingContext.getDiagnostics().forElement(element)
            if (!diagnostics.any { it.getFactory() in ERRORS }) return emptyList()

            val resolutionScope = element.getResolutionScope(bindingContext, file.getResolutionFacade())
            val containingDescriptor = resolutionScope.ownerDescriptor

            fun isVisible(descriptor: DeclarationDescriptor): Boolean {
                if (descriptor is DeclarationDescriptorWithVisibility) {
                    return descriptor.isVisible(containingDescriptor, bindingContext, element)
                }

                return true
            }

            val result = ArrayList<DeclarationDescriptor>()

            val indicesHelper = KotlinIndicesHelper(element.getResolutionFacade(), searchScope, ::isVisible, true)

            if (!element.isImportDirectiveExpression() && !JetPsiUtil.isSelectorInQualified(element)) {
                if (ProjectStructureUtil.isJsKotlinModule(file)) {
                    indicesHelper.getKotlinClasses({ it == referenceName }, { true }).filterTo(result, ::filterByCallType)

                }
                else {
                    indicesHelper.getJvmClassesByName(referenceName).filterTo(result, ::filterByCallType)
                }

                indicesHelper.getTopLevelCallablesByName(referenceName).filterTo(result, ::filterByCallType)
            }

            result.addAll(indicesHelper.getCallableTopLevelExtensions({ it == referenceName }, callTypeAndReceiver, element, bindingContext))

            return if (result.size() > 1)
                reduceCandidatesBasedOnDependencyRuleViolation(result, file)
            else
                result
        }

        private fun reduceCandidatesBasedOnDependencyRuleViolation(candidates: Collection<DeclarationDescriptor>, file: PsiFile): Collection<DeclarationDescriptor> {
            val project = file.project
            val validationManager = DependencyValidationManager.getInstance(project)
            return candidates.filter {
                val targetFile = DescriptorToSourceUtilsIde.getAnyDeclaration(project, it)?.containingFile ?: return@filter true
                validationManager.getViolatorDependencyRules(file, targetFile).isEmpty()
            }
        }
    }
}
