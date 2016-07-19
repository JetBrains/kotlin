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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInspection.HintAction
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
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.actions.KotlinAddImportAction
import org.jetbrains.kotlin.idea.actions.createSingleImportAction
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.getResolveScope
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isImportDirectiveExpression
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.CachedValueProperty
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.*

/**
 * Created by Semoro on 08.07.16.
 * ©XCodersTeam, 2016
 */

class AutoStaticImportFix(expression: KtSimpleNameExpression) :
        KotlinQuickFixAction<KtSimpleNameExpression>(expression), HighPriorityAction, HintAction {

    private val modificationCountOnCreate = PsiModificationTracker.SERVICE.getInstance(element.project).modificationCount

    private val suggestionCount: Int by CachedValueProperty(
            calculator = { computeSuggestions().size },
            timestampCalculator = { PsiModificationTracker.SERVICE.getInstance(element.project).modificationCount }
    )


    override fun showHint(editor: Editor): Boolean {
        if (!element.isValid || isOutdated()) return false

        if (ApplicationManager.getApplication().isUnitTestMode && HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)) return false

        if (suggestionCount == 0) return false

        return createAction(element.project, editor).showHint()
    }

    override fun getText() = KotlinBundle.message("import.static.fix")

    override fun getFamilyName() = KotlinBundle.message("import.fix")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile)
            = super.isAvailable(project, editor, file) && suggestionCount > 0

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        CommandProcessor.getInstance().runUndoTransparentAction {
            createAction(project, editor!!).execute()
        }
    }

    override fun startInWriteAction() = true

    private fun isOutdated() = modificationCountOnCreate != PsiModificationTracker.SERVICE.getInstance(element.project).modificationCount

    private fun createAction(project: Project, editor: Editor): KotlinAddImportAction {
        return createSingleImportAction(project, editor, element, computeSuggestions())
    }

    fun computeSuggestions(): Collection<DeclarationDescriptor> {
        if (!element.isValid) return listOf()
        if (element.containingFile !is KtFile) return emptyList()

        val callTypeAndReceiver = getCallTypeAndReceiver()

        if (callTypeAndReceiver is CallTypeAndReceiver.UNKNOWN) return emptyList()

        if (importNames.isEmpty()) return emptyList()

        return importNames.flatMapTo(LinkedHashSet()) {
            computeSuggestionsForName(it, callTypeAndReceiver)
        }
    }

    fun computeSuggestionsForName(name: Name, callTypeAndReceiver: CallTypeAndReceiver<*, *>):
            Collection<DeclarationDescriptor> {
        val nameStr = name.asString()
        if (nameStr.isEmpty()) return emptyList()

        val file = element.containingFile as KtFile

        fun filterByCallType(descriptor: DeclarationDescriptor) = callTypeAndReceiver.callType.descriptorKindFilter.accepts(descriptor)

        val searchScope = getResolveScope(file)

        val bindingContext = element.analyze(BodyResolveMode.PARTIAL)

        val diagnostics = bindingContext.diagnostics.forElement(element)

        if (!diagnostics.any { it.factory in getSupportedErrors() }) return emptyList()

        val resolutionFacade = element.getResolutionFacade()

        fun isVisible(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor is DeclarationDescriptorWithVisibility) {
                return descriptor.isVisible(element, callTypeAndReceiver.receiver as? KtExpression, bindingContext, resolutionFacade)
            }

            return true
        }

        val result = ArrayList<DeclarationDescriptor>()

        val indicesHelper = KotlinIndicesHelper(resolutionFacade, searchScope, ::isVisible)

        val expression = element
        if (expression is KtSimpleNameExpression) {
            if (!expression.isImportDirectiveExpression() && !KtPsiUtil.isSelectorInQualified(expression)) {
                indicesHelper.getKotlinStatics(nameStr).filterTo(result, ::filterByCallType)
                if (!(ProjectStructureUtil.isJsKotlinModule(file))) {
                    indicesHelper.getJvmStatics(nameStr).filterTo(result, ::filterByCallType)
                }
            }
        }
        return if (result.size > 1)
            reduceCandidatesBasedOnDependencyRuleViolation(result, file)
        else
            result
    }

    private fun reduceCandidatesBasedOnDependencyRuleViolation(
            candidates: Collection<DeclarationDescriptor>, file: PsiFile): Collection<DeclarationDescriptor> {
        val project = file.project
        val validationManager = DependencyValidationManager.getInstance(project)
        return candidates.filter {
            val targetFile = DescriptorToSourceUtilsIde.getAnyDeclaration(project, it)?.containingFile ?: return@filter true
            validationManager.getViolatorDependencyRules(file, targetFile).isEmpty()
        }
    }

    fun getCallTypeAndReceiver() = CallTypeAndReceiver.detect(element)

    val importNames: Collection<Name> = run {
        if (element.getIdentifier() == null) {
            val conventionName = KtPsiUtil.getConventionName(element)
            if (conventionName != null) {
                if (element is KtOperationReferenceExpression) {
                    val elementType = element.firstChild.node.elementType
                    if (elementType in OperatorConventions.ASSIGNMENT_OPERATIONS) {
                        val counterpart = OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS[elementType]
                        val counterpartName = OperatorConventions.BINARY_OPERATION_NAMES[counterpart]
                        if (counterpartName != null) {
                            return@run listOf(conventionName, counterpartName)
                        }
                    }
                }

                return@run conventionName.singletonOrEmptyList()
            }
        }
        else if (Name.isValidIdentifier(element.getReferencedName())) {
            return@run Name.identifier(element.getReferencedName()).singletonOrEmptyList()
        }

        emptyList<Name>()
    }

    fun getSupportedErrors(): Collection<DiagnosticFactory<*>> = ERRORS

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) =
                (diagnostic.psiElement as? KtSimpleNameExpression)?.let { AutoStaticImportFix(it) }

        override fun isApplicableForCodeFragment() = true

        private val ERRORS: Collection<DiagnosticFactory<*>> by lazy(LazyThreadSafetyMode.PUBLICATION) { QuickFixes.getInstance().getDiagnostics(this) }
    }
}
