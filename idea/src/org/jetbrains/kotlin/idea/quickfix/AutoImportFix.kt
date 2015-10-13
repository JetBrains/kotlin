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
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters2
import org.jetbrains.kotlin.diagnostics.Errors
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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isImportDirectiveExpression
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.CachedValueProperty
import java.util.*

/**
 * Check possibility and perform fix for unresolved references.
 */
abstract class AutoImportFixBase public constructor(
        expression: KtExpression,
        val diagnostics: Collection<Diagnostic> = emptyList()) : KotlinQuickFixAction<KtExpression>(expression), HighPriorityAction, HintAction {

    protected constructor(
            expression: KtExpression,
            diagnostic: Diagnostic? = null) : this(expression, if (diagnostic == null) emptyList() else listOf(diagnostic))

    private val modificationCountOnCreate = PsiModificationTracker.SERVICE.getInstance(element.getProject()).getModificationCount()

    @Volatile private var anySuggestionFound: Boolean? = null

    public val suggestions: Collection<DeclarationDescriptor> by CachedValueProperty(
            {
                val descriptors = computeSuggestions()
                anySuggestionFound = !descriptors.isEmpty()
                descriptors
            },
            { PsiModificationTracker.SERVICE.getInstance(element.getProject()).getModificationCount() })

    override fun showHint(editor: Editor): Boolean {
        if (!element.isValid() || isOutdated()) return false

        if (ApplicationManager.getApplication().isUnitTestMode() && HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)) return false

        if (suggestions.isEmpty()) return false

        val addImportAction = createAction(element.project, editor)
        val hintText = ShowAutoImportPass.getMessage(suggestions.size > 1, addImportAction.highestPriorityFqName.asString())
        HintManager.getInstance().showQuestionHint(editor, hintText, element.getTextOffset(), element.getTextRange()!!.getEndOffset(), addImportAction)

        return true
    }

    override fun getText() = JetBundle.message("import.fix")

    override fun getFamilyName() = JetBundle.message("import.fix")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile)
            = (super.isAvailable(project, editor, file)) && (anySuggestionFound ?: !suggestions.isEmpty())

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        CommandProcessor.getInstance().runUndoTransparentAction {
            createAction(project, editor!!).execute()
        }
    }

    override fun startInWriteAction() = true

    private fun isOutdated() = modificationCountOnCreate != PsiModificationTracker.SERVICE.getInstance(element.getProject()).getModificationCount()

    protected open fun createAction(project: Project, editor: Editor) = KotlinAddImportAction(project, editor, element as KtSimpleNameExpression, suggestions)

    protected fun computeSuggestions(): Collection<DeclarationDescriptor> {
        if (!element.isValid()) return listOf()

        val file = element.getContainingFile() as? KtFile ?: return emptyList()

        val callTypeAndReceiver = getTypeAndReceiver()

        if (callTypeAndReceiver is CallTypeAndReceiver.UNKNOWN) return emptyList()

        var referenceNames = getImportNames(diagnostics, element).filter { it.isNotEmpty() }
        if (referenceNames.isEmpty()) return emptyList()

        return referenceNames.flatMapTo(LinkedHashSet()) {
            Helper.computeSuggestionsForName(callTypeAndReceiver, element, file, it, getSupportedErrors())
        }
    }

    protected abstract fun getSupportedErrors(): Collection<DiagnosticFactory<*>>
    protected abstract fun getTypeAndReceiver(): CallTypeAndReceiver<*, *>
    protected abstract fun getImportNames(diagnostics: Collection<Diagnostic>, element: KtExpression): Collection<String>

    private object Helper {
        public fun computeSuggestionsForName(
                callTypeAndReceiver: CallTypeAndReceiver<out KtElement?, *>,
                expression: KtExpression, file: KtFile, referenceName: String,
                supportedErrors: Collection<DiagnosticFactory<*>>): Collection<DeclarationDescriptor> {
            fun filterByCallType(descriptor: DeclarationDescriptor) = callTypeAndReceiver.callType.descriptorKindFilter.accepts(descriptor)

            val searchScope = getResolveScope(file)

            val bindingContext = expression.analyze(BodyResolveMode.PARTIAL)

            val diagnostics = bindingContext.getDiagnostics().forElement(expression)

            if (!diagnostics.any { it.getFactory() in supportedErrors }) return emptyList()

            val resolutionScope = expression.getResolutionScope(bindingContext, file.getResolutionFacade())
            val containingDescriptor = resolutionScope.ownerDescriptor

            fun isVisible(descriptor: DeclarationDescriptor): Boolean {
                if (descriptor is DeclarationDescriptorWithVisibility) {
                    return descriptor.isVisible(containingDescriptor, bindingContext, expression as? KtSimpleNameExpression)
                }

                return true
            }

            val result = ArrayList<DeclarationDescriptor>()

            val indicesHelper = KotlinIndicesHelper(expression.getResolutionFacade(), searchScope, ::isVisible, true)

            if (expression is KtSimpleNameExpression) {
                if (!expression.isImportDirectiveExpression() && !KtPsiUtil.isSelectorInQualified(expression)) {
                    if (ProjectStructureUtil.isJsKotlinModule(file)) {
                        indicesHelper.getKotlinClasses({ it == referenceName }, { true }).filterTo(result, ::filterByCallType)

                    }
                    else {
                        indicesHelper.getJvmClassesByName(referenceName).filterTo(result, ::filterByCallType)
                    }

                    indicesHelper.getTopLevelCallablesByName(referenceName).filterTo(result, ::filterByCallType)
                }
            }

            result.addAll(indicesHelper.getCallableTopLevelExtensions({ it == referenceName }, callTypeAndReceiver, expression, bindingContext))

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
    }
}

class AutoImportFix(expression: KtSimpleNameExpression, diagnostic: Diagnostic? = null) : AutoImportFixBase(expression, diagnostic) {
    override fun getTypeAndReceiver(): CallTypeAndReceiver<*, *> = CallTypeAndReceiver.detect(element as JetSimpleNameExpression)
    override fun getImportNames(diagnostics: Collection<Diagnostic>, element: KtExpression): Collection<String> {
        element as KtSimpleNameExpression

        if (element.getIdentifier() == null) {
            val conventionName = KtPsiUtil.getConventionName(element)
            if (conventionName != null) {
                if (element is KtOperationReferenceExpression) {
                    val elementType = element.firstChild.node.elementType
                    if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(elementType)) {
                        val conterpart = OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.get(elementType)
                        val counterpartName = OperatorConventions.BINARY_OPERATION_NAMES.get(conterpart)
                        if (counterpartName != null) {
                            return listOf(conventionName.asString(), counterpartName.asString())
                        }
                    }
                }

                return listOf(conventionName.asString())
            }
        }
        else {
            return listOf(element.getReferencedName())
        }

        return emptyList()
    }

    override fun getSupportedErrors() = ERRORS

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtExpression>? {
            // There could be different psi elements (i.e. JetArrayAccessExpression), but we can fix only JetSimpleNameExpression case
            val psiElement = diagnostic.getPsiElement()
            if (psiElement is KtSimpleNameExpression) {
                return AutoImportFix(psiElement, diagnostic)
            }

            return null
        }

        override fun isApplicableForCodeFragment() = true

        private val ERRORS: Collection<DiagnosticFactory<*>> by lazy(LazyThreadSafetyMode.PUBLICATION) { QuickFixes.getInstance().getDiagnostics(this) }
    }
}

class MissingInvokeAutoImportFix(expression: KtExpression, diagnostic: Diagnostic) : AutoImportFixBase(expression, diagnostic) {
    override fun getImportNames(diagnostics: Collection<Diagnostic>, element: KtExpression) = setOf("invoke")

    override fun getTypeAndReceiver() = CallTypeAndReceiver.OPERATOR(element as KtExpression)

    override fun getSupportedErrors() = ERRORS

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtExpression>? {
            val element = diagnostic.psiElement
            if (element is KtExpression) {
                return MissingInvokeAutoImportFix(element, diagnostic)
            }

            return null
        }

        private val ERRORS by lazy(LazyThreadSafetyMode.PUBLICATION) { QuickFixes.getInstance().getDiagnostics(this) }
    }
}

class MissingArrayAccessorAutoImportFix(element: KtArrayAccessExpression, diagnostic: Diagnostic) : AutoImportFixBase(element, diagnostic) {
    override fun getImportNames(diagnostics: Collection<Diagnostic>, element: KtExpression): Set<String> {
        val s = if ((element.parent as? KtBinaryExpression)?.operationToken == KtTokens.EQ) "set" else "get"
        return setOf(s)
    }

    override fun getTypeAndReceiver() =
            CallTypeAndReceiver.OPERATOR((element as KtArrayAccessExpression).arrayExpression!!)

    override fun getSupportedErrors() = ERRORS

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtExpression>? {
            val element = diagnostic.psiElement
            if (element is KtArrayAccessExpression && element.arrayExpression != null) {
                return MissingArrayAccessorAutoImportFix(element, diagnostic)
            }

            return null
        }

        private val ERRORS by lazy(LazyThreadSafetyMode.PUBLICATION) { QuickFixes.getInstance().getDiagnostics(this) }
    }
}

class MissingDelegateAccessorsAutoImportFix(element: KtExpression, diagnostics: Collection<Diagnostic>) : AutoImportFixBase(element, diagnostics) {
    override fun createAction(project: Project, editor: Editor): KotlinAddImportAction {
        return KotlinAddImportAction(project, editor, element, suggestions)
    }

    override fun getImportNames(diagnostics: Collection<Diagnostic>, element: KtExpression): Set<String> {
        return diagnostics.mapTo(LinkedHashSet()) { if (it.toString().contains("setValue")) "setValue" else "getValue" }
    }

    override fun getTypeAndReceiver() = CallTypeAndReceiver.DELEGATE(element as KtExpression)

    override fun getSupportedErrors() = ERRORS

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtExpression>? {
            assert(diagnostic.factory == Errors.DELEGATE_SPECIAL_FUNCTION_MISSING)
            return MissingDelegateAccessorsAutoImportFix(diagnostic.psiElement as KtExpression, listOf(diagnostic))
        }

        private val ERRORS by lazy(LazyThreadSafetyMode.PUBLICATION) { QuickFixes.getInstance().getDiagnostics(this) }
    }
}

class MissingComponentsAutoImportFix(element: KtExpression, diagnostics: Collection<Diagnostic>) : AutoImportFixBase(element, diagnostics) {
    override fun createAction(project: Project, editor: Editor): KotlinAddImportAction {
        return KotlinAddImportAction(project, editor, element, suggestions)
    }

    override fun getImportNames(diagnostics: Collection<Diagnostic>, element: KtExpression): List<String> {
        return diagnostics.map {
            @Suppress("UNCHECKED_CAST")
            (it as DiagnosticWithParameters2<*, Name, *>).a.identifier
        }
    }

    override fun getTypeAndReceiver() = CallTypeAndReceiver.OPERATOR(element as KtExpression)

    override fun getSupportedErrors() = ERRORS

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtExpression>? {
            assert(diagnostic.factory == Errors.COMPONENT_FUNCTION_MISSING)
            return MissingComponentsAutoImportFix(diagnostic.psiElement as KtExpression, listOf(diagnostic))
        }

        private val ERRORS by lazy(LazyThreadSafetyMode.PUBLICATION) { QuickFixes.getInstance().getDiagnostics(this) }
    }
}