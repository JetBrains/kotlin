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

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.HintAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.packageDependencies.DependencyValidationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.actions.KotlinAddImportAction
import org.jetbrains.kotlin.idea.actions.createGroupedImportsAction
import org.jetbrains.kotlin.idea.actions.createSingleImportAction
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.getResolveScope
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.getConventionName
import org.jetbrains.kotlin.psi.KtPsiUtil.isSelectorInQualified
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isImportDirectiveExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils.isTopLevelDeclaration
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentCall
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.ExplicitImportsScope
import org.jetbrains.kotlin.resolve.scopes.utils.addImportingScope
import org.jetbrains.kotlin.resolve.scopes.utils.collectFunctions
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.CachedValueProperty
import org.jetbrains.kotlin.utils.addToStdlib.singletonList
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.lang.IllegalStateException
import java.util.*

/**
 * Check possibility and perform fix for unresolved references.
 */
internal abstract class ImportFixBase<T : KtExpression>(expression: T) :
        KotlinQuickFixAction<T>(expression), HighPriorityAction, HintAction {

    private val modificationCountOnCreate = PsiModificationTracker.SERVICE.getInstance(element.project).modificationCount

    private val suggestionCount: Int by CachedValueProperty(
            calculator = { computeSuggestions().size },
            timestampCalculator = { PsiModificationTracker.SERVICE.getInstance(element.project).modificationCount }
    )

    protected abstract val importNames: Collection<Name>
    protected abstract fun getSupportedErrors(): Collection<DiagnosticFactory<*>>
    protected abstract fun getCallTypeAndReceiver(): CallTypeAndReceiver<*, *>

    override fun showHint(editor: Editor): Boolean {
        if (!element.isValid || isOutdated()) return false

        if (ApplicationManager.getApplication().isUnitTestMode && HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)) return false

        if (suggestionCount == 0) return false

        return createAction(element.project, editor).showHint()
    }

    override fun getText() = KotlinBundle.message("import.fix")

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

    protected open fun createAction(project: Project, editor: Editor): KotlinAddImportAction {
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

    private fun computeSuggestionsForName(name: Name, callTypeAndReceiver: CallTypeAndReceiver<*, *>): Collection<DeclarationDescriptor> {
        val nameStr = name.asString()
        if (nameStr.isEmpty()) return emptyList()

        val file = element.getContainingKtFile()

        val bindingContext = element.analyze(BodyResolveMode.PARTIAL)
        if (!checkErrorStillPresent(bindingContext)) return emptyList()

        val searchScope = getResolveScope(file)

        val resolutionFacade = file.getResolutionFacade()

        fun isVisible(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor is DeclarationDescriptorWithVisibility) {
                return descriptor.isVisible(element, callTypeAndReceiver.receiver as? KtExpression, bindingContext, resolutionFacade)
            }

            return true
        }

        val indicesHelper = KotlinIndicesHelper(resolutionFacade, searchScope, ::isVisible)

        var result = fillCandidates(nameStr, callTypeAndReceiver, bindingContext, indicesHelper)

        // for CallType.DEFAULT do not include functions if there is no parenthesis
        if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
            val isCall = element.parent is KtCallExpression
            if (!isCall) {
                result = result.filter { it !is FunctionDescriptor }
            }
        }

        return if (result.size > 1)
            reduceCandidatesBasedOnDependencyRuleViolation(result, file)
        else
            result
    }

    private fun checkErrorStillPresent(bindingContext: BindingContext): Boolean {
        return elementsToCheckDiagnostics()
                .flatMap { bindingContext.diagnostics.forElement(it) }
                .any { diagnostic -> diagnostic.factory in getSupportedErrors() }
    }

    protected open fun elementsToCheckDiagnostics(): Collection<PsiElement> = listOf(element)

    abstract fun fillCandidates(
            name: String,
            callTypeAndReceiver: CallTypeAndReceiver<*, *>,
            bindingContext: BindingContext,
            indicesHelper: KotlinIndicesHelper
    ): List<DeclarationDescriptor>

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

internal abstract class OrdinaryImportFixBase<T : KtExpression>(expression: T) : ImportFixBase<T>(expression) {
    override fun fillCandidates(
            name: String,
            callTypeAndReceiver: CallTypeAndReceiver<*, *>,
            bindingContext: BindingContext,
            indicesHelper: KotlinIndicesHelper
    ): List<DeclarationDescriptor> {
        val result = ArrayList<DeclarationDescriptor>()

        val expression = element

        if (expression is KtSimpleNameExpression) {
            if (!expression.isImportDirectiveExpression() && !isSelectorInQualified(expression)) {
                val filterByCallType = { descriptor: DeclarationDescriptor -> callTypeAndReceiver.callType.descriptorKindFilter.accepts(descriptor) }

                if (ProjectStructureUtil.isJsKotlinModule(expression.getContainingKtFile())) {
                    indicesHelper.getKotlinClasses({ it == name }, { true }).filterTo(result, filterByCallType)
                }
                else {
                    indicesHelper.getJvmClassesByName(name).filterTo(result, filterByCallType)
                }

                indicesHelper.getTopLevelTypeAliases { it == name }.filterTo(result, filterByCallType)

                indicesHelper.getTopLevelCallablesByName(name).filterTo(result, filterByCallType)
            }
        }

        result.addAll(indicesHelper.getCallableTopLevelExtensions(callTypeAndReceiver, expression, bindingContext) { it == name })
        return result
    }

}

internal class ImportFix(expression: KtSimpleNameExpression) : OrdinaryImportFixBase<KtSimpleNameExpression>(expression) {
    override fun getCallTypeAndReceiver() = CallTypeAndReceiver.detect(element)

    override val importNames = element.extractImportNames()

    override fun getSupportedErrors() = ERRORS

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) =
                (diagnostic.psiElement as? KtSimpleNameExpression)?.let { ImportFix(it) }

        override fun isApplicableForCodeFragment() = true

        private val ERRORS: Collection<DiagnosticFactory<*>> by lazy { QuickFixes.getInstance().getDiagnostics(this) }
    }
}

private fun KtSimpleNameExpression.extractImportNames(): Collection<Name> {
    if (getIdentifier() == null) {
        val conventionName = getConventionName(this)
        if (conventionName != null) {
            if (this is KtOperationReferenceExpression) {
                val elementType = firstChild.node.elementType
                if (elementType in OperatorConventions.ASSIGNMENT_OPERATIONS) {
                    val counterpart = OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS[elementType]
                    val counterpartName = OperatorConventions.BINARY_OPERATION_NAMES[counterpart]
                    if (counterpartName != null) {
                        return listOf(conventionName, counterpartName)
                    }
                }
            }

            return conventionName.singletonOrEmptyList()
        }
    }
    else if (Name.isValidIdentifier(getReferencedName())) {
        return Name.identifier(getReferencedName()).singletonOrEmptyList()
    }

    return emptyList()
}


internal class InvokeImportFix(expression: KtExpression) : OrdinaryImportFixBase<KtExpression>(expression) {
    override val importNames = OperatorNameConventions.INVOKE.singletonList()

    override fun getCallTypeAndReceiver() = CallTypeAndReceiver.OPERATOR(element)

    override fun getSupportedErrors() = ERRORS

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) =
                (diagnostic.psiElement as? KtExpression)?.let { InvokeImportFix(it) }

        private val ERRORS by lazy { QuickFixes.getInstance().getDiagnostics(this) }
    }
}

internal open class ArrayAccessorImportFix(
        element: KtArrayAccessExpression,
        override val importNames: Collection<Name>,
        private val showHint: Boolean
) : OrdinaryImportFixBase<KtArrayAccessExpression>(element) {

    override fun getCallTypeAndReceiver() =
            CallTypeAndReceiver.OPERATOR(element.arrayExpression!!)

    override fun getSupportedErrors() = ERRORS

    override fun showHint(editor: Editor) = showHint && super.showHint(editor)

    companion object : KotlinSingleIntentionActionFactory() {
        private fun importName(diagnostic: Diagnostic): Name {
            return when (diagnostic.factory) {
                Errors.NO_GET_METHOD -> OperatorNameConventions.GET
                Errors.NO_SET_METHOD -> OperatorNameConventions.SET
                else -> throw IllegalStateException("Shouldn't be called for other diagnostics")
            }
        }

        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtArrayAccessExpression>? {
            val factory = diagnostic.factory
            assert(factory == Errors.NO_GET_METHOD || factory == Errors.NO_SET_METHOD)

            val element = diagnostic.psiElement
            if (element is KtArrayAccessExpression && element.arrayExpression != null) {
                return ArrayAccessorImportFix(element, importName(diagnostic).singletonList(), true)
            }

            return null
        }

        private val ERRORS by lazy { QuickFixes.getInstance().getDiagnostics(this) }
    }
}

internal class DelegateAccessorsImportFix(
        element: KtExpression,
        override val importNames: Collection<Name>,
        private val solveSeveralProblems: Boolean
) : OrdinaryImportFixBase<KtExpression>(element) {

    override fun getCallTypeAndReceiver() = CallTypeAndReceiver.DELEGATE(element)

    override fun createAction(project: Project, editor: Editor): KotlinAddImportAction {
        if (solveSeveralProblems) {
            return createGroupedImportsAction(project, editor, element, "Delegate accessors", computeSuggestions())
        }

        return super.createAction(project, editor)
    }

    override fun getSupportedErrors() = ERRORS

    companion object : KotlinSingleIntentionActionFactory() {
        private fun importNames(diagnostics: Collection<Diagnostic>): Collection<Name> {
            return diagnostics.map {
                val missingMethodSignature = Errors.DELEGATE_SPECIAL_FUNCTION_MISSING.cast(it).a
                if (missingMethodSignature.startsWith(OperatorNameConventions.GET_VALUE.identifier))
                    OperatorNameConventions.GET_VALUE
                else
                    OperatorNameConventions.SET_VALUE
            }.distinct()
        }

        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtExpression>? {
            return (diagnostic.psiElement as? KtExpression)?.let {
                DelegateAccessorsImportFix(it, importNames(diagnostic.singletonList()), false)
            }
        }

        override fun doCreateActionsForAllProblems(sameTypeDiagnostics: Collection<Diagnostic>): List<IntentionAction> {
            val element = sameTypeDiagnostics.first().psiElement
            val names = importNames(sameTypeDiagnostics)
            return (element as? KtExpression)?.let { DelegateAccessorsImportFix(it, names, true) }.singletonOrEmptyList()
        }

        private val ERRORS by lazy { QuickFixes.getInstance().getDiagnostics(this) }
    }
}

internal class ComponentsImportFix(
        element: KtExpression,
        override val importNames: Collection<Name>,
        private val solveSeveralProblems: Boolean
) : OrdinaryImportFixBase<KtExpression>(element) {

    override fun getCallTypeAndReceiver() = CallTypeAndReceiver.OPERATOR(element)

    override fun createAction(project: Project, editor: Editor): KotlinAddImportAction {
        if (solveSeveralProblems) {
            return createGroupedImportsAction(project, editor, element, "Component functions", computeSuggestions())
        }

        return super.createAction(project, editor)
    }

    override fun getSupportedErrors() = ERRORS

    companion object : KotlinSingleIntentionActionFactory() {
        private fun importNames(diagnostics: Collection<Diagnostic>) =
                diagnostics.map { Name.identifier(Errors.COMPONENT_FUNCTION_MISSING.cast(it).a.identifier) }

        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtExpression>? {
            return (diagnostic.psiElement as? KtExpression)?.let {
                ComponentsImportFix(it, importNames(diagnostic.singletonList()), false)
            }
        }

        override fun doCreateActionsForAllProblems(sameTypeDiagnostics: Collection<Diagnostic>): List<IntentionAction> {
            val element = sameTypeDiagnostics.first().psiElement
            val names = importNames(sameTypeDiagnostics)
            val solveSeveralProblems = sameTypeDiagnostics.size > 1
            return (element as? KtExpression)?.let { ComponentsImportFix(it, names, solveSeveralProblems) }.singletonOrEmptyList()
        }

        private val ERRORS by lazy { QuickFixes.getInstance().getDiagnostics(this) }
    }
}

internal class ImportMemberFix(expression: KtSimpleNameExpression) : ImportFixBase<KtSimpleNameExpression>(expression) {

    override fun getText() = "Import member"

    override fun fillCandidates(
            name: String,
            callTypeAndReceiver: CallTypeAndReceiver<*, *>,
            bindingContext: BindingContext,
            indicesHelper: KotlinIndicesHelper
    ): List<DeclarationDescriptor> {
        val result = ArrayList<DeclarationDescriptor>()
        if (!element.isImportDirectiveExpression() && !isSelectorInQualified(element)) {
            val filterByCallType = { descriptor: DeclarationDescriptor -> callTypeAndReceiver.callType.descriptorKindFilter.accepts(descriptor) }

            indicesHelper.getKotlinCallablesByName(name)
                    .filter { it.canBeReferencedViaImport() && !isTopLevelDeclaration(it) }
                    .filterTo(result, filterByCallType)

            if (!ProjectStructureUtil.isJsKotlinModule(element.getContainingKtFile())) {
                indicesHelper.getJvmCallablesByName(name)
                        .filter { it.canBeReferencedViaImport() }
                        .filterTo(result, filterByCallType)
            }
        }
        return result
    }

    override fun getCallTypeAndReceiver() = CallTypeAndReceiver.detect(element)

    override val importNames: Collection<Name> = run {
        if (element.getIdentifier() != null) {
            val name = element.getReferencedName()
            if (Name.isValidIdentifier(name)) {
                return@run Name.identifier(name).singletonList()
            }
        }
        return@run emptyList()
    }

    override fun getSupportedErrors(): Collection<DiagnosticFactory<*>> = ERRORS

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) =
                (diagnostic.psiElement as? KtSimpleNameExpression)?.let(::ImportMemberFix)

        override fun isApplicableForCodeFragment() = true

        private val ERRORS: Collection<DiagnosticFactory<*>> by lazy { QuickFixes.getInstance().getDiagnostics(this) }
    }
}

internal class ImportForMismatchingArgumentsFix(expression: KtSimpleNameExpression) : ImportFixBase<KtSimpleNameExpression>(expression) {
    override fun getCallTypeAndReceiver() = CallTypeAndReceiver.detect(element)

    override val importNames = element.extractImportNames()

    override fun getSupportedErrors() = ERRORS

    override fun elementsToCheckDiagnostics(): Collection<PsiElement> {
        val callExpression = element.parent as? KtCallExpression ?: return emptyList()
        return callExpression.valueArguments +
               callExpression.valueArguments.mapNotNull { it.getArgumentExpression() } +
               callExpression.valueArguments.mapNotNull { it.getArgumentName()?.referenceExpression } +
               callExpression.valueArgumentList.singletonOrEmptyList()
    }

    override fun fillCandidates(
            name: String,
            callTypeAndReceiver: CallTypeAndReceiver<*, *>,
            bindingContext: BindingContext,
            indicesHelper: KotlinIndicesHelper
    ): List<DeclarationDescriptor> {
        if (!Name.isValidIdentifier(name)) return emptyList()
        val identifier = Name.identifier(name)

        val call = element.getParentCall(bindingContext) ?: return emptyList()
        val callElement = call.callElement as? KtExpression ?: return emptyList()
        if (callElement.anyDescendantOfType<PsiErrorElement>()) return emptyList() // incomplete call
        val elementToAnalyze = callElement.getQualifiedExpressionForSelectorOrThis()

        val file = element.getContainingKtFile()
        val resolutionFacade = file.getResolutionFacade()
        val resolutionScope = elementToAnalyze.getResolutionScope(bindingContext, resolutionFacade)

        fun filterFunction(descriptor: FunctionDescriptor): Boolean {
            if (!callTypeAndReceiver.callType.descriptorKindFilter.accepts(descriptor)) return false

            if (descriptor in resolutionScope.collectFunctions(identifier, NoLookupLocation.FROM_IDE)) return false // already imported

            // check that this function matches all arguments
            val resolutionScopeWithAddedImport = resolutionScope.addImportingScope(ExplicitImportsScope(listOf(descriptor)))
            val dataFlowInfo = bindingContext.getDataFlowInfo(elementToAnalyze)
            val newBindingContext = elementToAnalyze.analyzeInContext(
                    resolutionScopeWithAddedImport,
                    dataFlowInfo = dataFlowInfo,
                    contextDependency = ContextDependency.DEPENDENT // to not check complete inference
            )
            return newBindingContext.diagnostics.none { it.severity == Severity.ERROR }
        }

        val result = ArrayList<FunctionDescriptor>()

        fun processDescriptor(descriptor: CallableDescriptor) {
            if (descriptor is FunctionDescriptor && filterFunction(descriptor)) {
                result.add(descriptor)
            }
        }

        indicesHelper
                .getCallableTopLevelExtensions(callTypeAndReceiver, element, bindingContext) { it == name }
                .forEach(::processDescriptor)

        if (!isSelectorInQualified(element)) {
            indicesHelper
                    .getTopLevelCallablesByName(name)
                    .forEach(::processDescriptor)
        }

        return result
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): ImportForMismatchingArgumentsFix? {
            //TODO: not only KtCallExpression
            val callExpression = diagnostic.psiElement.getStrictParentOfType<KtCallExpression>() ?: return null
            val nameExpression = callExpression.calleeExpression as? KtNameReferenceExpression ?: return null
            return ImportForMismatchingArgumentsFix(nameExpression)
        }

        override fun isApplicableForCodeFragment() = true

        private val ERRORS: Collection<DiagnosticFactory<*>> by lazy { QuickFixes.getInstance().getDiagnostics(this) }
    }
}

object ImportForMissingOperatorFactory : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val element = diagnostic.psiElement as? KtExpression ?: return null
        val operatorDescriptor = Errors.OPERATOR_MODIFIER_REQUIRED.cast(diagnostic).a
        val name = operatorDescriptor.name
        when (name) {
            OperatorNameConventions.GET, OperatorNameConventions.SET -> {
                if (element is KtArrayAccessExpression) {
                    return object : ArrayAccessorImportFix(element, name.singletonList(), false) {
                        override fun getSupportedErrors() = Errors.OPERATOR_MODIFIER_REQUIRED.singletonList()
                    }
                }
            }
        }

        return null
    }
}