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

import com.intellij.codeInsight.ImportFilter
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
import com.intellij.psi.PsiModifier
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.actions.KotlinAddImportAction
import org.jetbrains.kotlin.idea.actions.createGroupedImportsAction
import org.jetbrains.kotlin.idea.actions.createSingleImportAction
import org.jetbrains.kotlin.idea.actions.createSingleImportActionForConstructor
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.getResolveScope
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.isSelectorInQualified
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentCall
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.ExplicitImportsScope
import org.jetbrains.kotlin.resolve.scopes.utils.addImportingScope
import org.jetbrains.kotlin.resolve.scopes.utils.collectFunctions
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.ifEmpty
import java.util.*

/**
 * Check possibility and perform fix for unresolved references.
 */
internal abstract class ImportFixBase<T : KtExpression> protected constructor(
        expression: T,
        private val factory: Factory
) : KotlinQuickFixAction<T>(expression), HighPriorityAction, HintAction {
    private val project = expression.project

    private val modificationCountOnCreate = PsiModificationTracker.SERVICE.getInstance(project).modificationCount

    protected lateinit var suggestions: Collection<FqName>

    fun computeSuggestions() {
        suggestions = collectSuggestions()
    }

    protected open fun getSupportedErrors() = factory.supportedErrors

    protected abstract val importNames: Collection<Name>
    protected abstract fun getCallTypeAndReceiver(): CallTypeAndReceiver<*, *>?

    override fun showHint(editor: Editor): Boolean {
        val element = element ?: return false

        if (!element.isValid || isOutdated()) return false

        if (ApplicationManager.getApplication().isUnitTestMode && HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)) return false

        if (suggestions.isEmpty()) return false

        return createAction(project, editor, element).showHint()
    }

    override fun getText() = KotlinBundle.message("import.fix")

    override fun getFamilyName() = KotlinBundle.message("import.fix")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile)
            = element != null && super.isAvailable(project, editor, file) && suggestions.isNotEmpty()

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        CommandProcessor.getInstance().runUndoTransparentAction {
            createAction(project, editor!!, element).execute()
        }
    }

    override fun startInWriteAction() = true

    private fun isOutdated() = modificationCountOnCreate != PsiModificationTracker.SERVICE.getInstance(project).modificationCount

    protected open fun createAction(project: Project, editor: Editor, element: KtExpression): KotlinAddImportAction {
        return createSingleImportAction(project, editor, element, suggestions)
    }

    fun collectSuggestions(): Collection<FqName> {
        val element = element ?: return emptyList()
        if (!element.isValid) return emptyList()
        if (element.containingFile !is KtFile) return emptyList()

        val callTypeAndReceiver = getCallTypeAndReceiver() ?: return emptyList()

        if (callTypeAndReceiver is CallTypeAndReceiver.UNKNOWN) return emptyList()

        if (importNames.isEmpty()) return emptyList()

        return importNames
                .flatMap { collectSuggestionsForName(it, callTypeAndReceiver) }
                .distinct()
                .map { it.fqNameSafe }
                .distinct()
    }

    private fun collectSuggestionsForName(name: Name, callTypeAndReceiver: CallTypeAndReceiver<*, *>): Collection<DeclarationDescriptor> {
        val element = element ?: return emptyList()
        val nameStr = name.asString()
        if (nameStr.isEmpty()) return emptyList()

        val file = element.containingKtFile

        val bindingContext = element.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
        if (!checkErrorStillPresent(bindingContext)) return emptyList()

        val searchScope = getResolveScope(file)

        val resolutionFacade = file.getResolutionFacade()

        fun isVisible(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor is DeclarationDescriptorWithVisibility) {
                return descriptor.isVisible(element, callTypeAndReceiver.receiver as? KtExpression, bindingContext, resolutionFacade)
            }

            return true
        }

        val indicesHelper = KotlinIndicesHelper(resolutionFacade, searchScope, ::isVisible, file = file)

        var result = fillCandidates(nameStr, callTypeAndReceiver, bindingContext, indicesHelper)

        // for CallType.DEFAULT do not include functions if there is no parenthesis
        if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
            val isCall = element.parent is KtCallExpression
            if (!isCall) {
                result = result.filter { it !is FunctionDescriptor }
            }
        }

        result = result.filter { ImportFilter.shouldImport(file, it.fqNameSafe.asString()) }

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

    protected open fun elementsToCheckDiagnostics(): Collection<PsiElement> = listOfNotNull(element)

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

    abstract class Factory : KotlinSingleIntentionActionFactory() {
        val supportedErrors: Collection<DiagnosticFactory<*>> by lazy { QuickFixes.getInstance().getDiagnostics(this) }

        override fun isApplicableForCodeFragment() = true

        abstract fun createImportAction(diagnostic: Diagnostic): ImportFixBase<*>?

        open fun createImportActionsForAllProblems(sameTypeDiagnostics: Collection<Diagnostic>): List<ImportFixBase<*>> = emptyList()

        override final fun createAction(diagnostic: Diagnostic): IntentionAction? {
            return createImportAction(diagnostic)?.apply { computeSuggestions() }
        }

        override final fun doCreateActionsForAllProblems(sameTypeDiagnostics: Collection<Diagnostic>): List<IntentionAction> {
            return createImportActionsForAllProblems(sameTypeDiagnostics).onEach { it.computeSuggestions() }
        }
    }

}

internal abstract class OrdinaryImportFixBase<T : KtExpression>(expression: T, factory: Factory) : ImportFixBase<T>(expression, factory) {
    override fun fillCandidates(
            name: String,
            callTypeAndReceiver: CallTypeAndReceiver<*, *>,
            bindingContext: BindingContext,
            indicesHelper: KotlinIndicesHelper
    ): List<DeclarationDescriptor> {
        val expression = element ?: return emptyList()

        val result = ArrayList<DeclarationDescriptor>()

        if (expression is KtSimpleNameExpression) {
            if (!expression.isImportDirectiveExpression() && !isSelectorInQualified(expression)) {
                val filterByCallType = callTypeAndReceiver.toFilter()

                indicesHelper.getClassesByName(expression, name).filterTo(result, filterByCallType)

                indicesHelper.getTopLevelTypeAliases { it == name }.filterTo(result, filterByCallType)

                indicesHelper.getTopLevelCallablesByName(name).filterTo(result, filterByCallType)
            }
        }

        result.addAll(indicesHelper.getCallableTopLevelExtensions(callTypeAndReceiver, expression, bindingContext) { it == name })
        return result
    }
}

internal class ImportFix(expression: KtSimpleNameExpression) : OrdinaryImportFixBase<KtSimpleNameExpression>(expression, MyFactory) {
    override fun getCallTypeAndReceiver() = element?.let { CallTypeAndReceiver.detect(it) }

    private fun importNamesForMembers(): Collection<Name> {
        val element = element ?: return emptyList()

        if (element.getIdentifier() != null) {
            val name = element.getReferencedName()
            if (Name.isValidIdentifier(name)) {
                return listOf(Name.identifier(name))
            }
        }

        return emptyList()
    }

    override val importNames: Collection<Name> = ((element?.mainReference?.resolvesByNames ?: emptyList()) + importNamesForMembers()).distinct()

    private fun collectMemberCandidates(
            name: String,
            callTypeAndReceiver: CallTypeAndReceiver<*, *>,
            bindingContext: BindingContext,
            indicesHelper: KotlinIndicesHelper
    ): List<DeclarationDescriptor> {

        val element = element ?: return emptyList()
        if (element.isImportDirectiveExpression() || isSelectorInQualified(element)) return emptyList()

        val result = ArrayList<DeclarationDescriptor>()

        val filterByCallType = callTypeAndReceiver.toFilter()

        indicesHelper.getKotlinEnumsByName(name).filterTo(result, filterByCallType)

        val resolutionFacade = element.getResolutionFacade()
        var actualReceiverTypes = callTypeAndReceiver
                .receiverTypesWithIndex(bindingContext, element,
                                        resolutionFacade.moduleDescriptor, resolutionFacade,
                                        stableSmartCastsOnly = false,
                                        withImplicitReceiversWhenExplicitPresent = true).orEmpty()

        if (element.languageVersionSettings.supportsFeature(LanguageFeature.DslMarkersSupport)) {
            actualReceiverTypes -= actualReceiverTypes.shadowedByDslMarkers()
        }

        val explicitReceiverTypes = actualReceiverTypes.filterNot { it.implicit }

        val checkDispatchReceiver = when(callTypeAndReceiver) {
            is CallTypeAndReceiver.OPERATOR, is CallTypeAndReceiver.INFIX -> true
            else -> false
        }

        val processor = { descriptor: CallableDescriptor ->
            if (descriptor.canBeReferencedViaImport() && filterByCallType(descriptor)
                && descriptor.isValidByReceiversFor(explicitReceiverTypes, actualReceiverTypes, checkDispatchReceiver)) {
                result.add(descriptor)
            }
        }

        indicesHelper.processKotlinCallablesByName(
                name,
                filter = { declaration -> (declaration.parent as? KtClassBody)?.parent is KtObjectDeclaration },
                processor = processor
        )

        if (TargetPlatformDetector.getPlatform(element.containingKtFile) == JvmPlatform) {
            indicesHelper.processJvmCallablesByName(
                    name,
                    filter = { it.hasModifierProperty(PsiModifier.STATIC) },
                    processor = processor
            )
        }
        return result
    }


    private fun CallableDescriptor.isValidByReceiversFor(explicitReceiverTypes: Collection<ReceiverType>,
                                                         allReceiverTypes: Collection<ReceiverType>,
                                                         checkDispatchReceiver: Boolean): Boolean {
        val bothReceivers = listOfNotNull(extensionReceiverParameter, dispatchReceiverParameter.takeIf { checkDispatchReceiver })

        val receiverTypesPerReceiver = generateSequence(explicitReceiverTypes.ifEmpty { allReceiverTypes }) { allReceiverTypes }

        return bothReceivers
                .zip(receiverTypesPerReceiver.asIterable())
                .all { (receiver, possibleTypes) -> possibleTypes.any { it.type.isSubtypeOf(receiver.type) } }
    }

    override fun fillCandidates(
            name: String,
            callTypeAndReceiver: CallTypeAndReceiver<*, *>,
            bindingContext: BindingContext,
            indicesHelper: KotlinIndicesHelper
    ): List<DeclarationDescriptor> {
        return super.fillCandidates(name, callTypeAndReceiver, bindingContext, indicesHelper) + collectMemberCandidates(name, callTypeAndReceiver, bindingContext, indicesHelper)
    }

    companion object MyFactory : Factory() {
        override fun createImportAction(diagnostic: Diagnostic) =
                (diagnostic.psiElement as? KtSimpleNameExpression)?.let(::ImportFix)
    }
}

internal class ImportConstructorReferenceFix(expression: KtSimpleNameExpression) : ImportFixBase<KtSimpleNameExpression>(expression, MyFactory) {
    override fun getCallTypeAndReceiver() = element?.let {
        CallTypeAndReceiver.detect(it) as? CallTypeAndReceiver.CALLABLE_REFERENCE
    }

    override fun fillCandidates(name: String, callTypeAndReceiver: CallTypeAndReceiver<*, *>, bindingContext: BindingContext, indicesHelper: KotlinIndicesHelper): List<DeclarationDescriptor> {
        val expression = element ?: return emptyList()

        val filterByCallType = callTypeAndReceiver.toFilter()
        // TODO Type-aliases
        return indicesHelper.getClassesByName(expression, name)
                .asSequence()
                .map { it.constructors }.flatten()
                .filter { it.importableFqName != null }
                .filter(filterByCallType)
                .toList()
    }

    override fun createAction(project: Project, editor: Editor, element: KtExpression): KotlinAddImportAction {
        return createSingleImportActionForConstructor(project, editor, element, suggestions)
    }

    override val importNames = element?.mainReference?.resolvesByNames ?: emptyList()

    companion object MyFactory : Factory() {
        override fun createImportAction(diagnostic: Diagnostic) =
                (diagnostic.psiElement as? KtSimpleNameExpression)?.let(::ImportConstructorReferenceFix)
    }
}

internal class InvokeImportFix(expression: KtExpression) : OrdinaryImportFixBase<KtExpression>(expression, MyFactory) {
    override val importNames = listOf(OperatorNameConventions.INVOKE)

    override fun getCallTypeAndReceiver() = element?.let { CallTypeAndReceiver.OPERATOR(it) }

    companion object MyFactory : Factory() {
        override fun createImportAction(diagnostic: Diagnostic) =
                (diagnostic.psiElement as? KtExpression)?.let(::InvokeImportFix)
    }
}

internal open class ArrayAccessorImportFix(
        element: KtArrayAccessExpression,
        override val importNames: Collection<Name>,
        private val showHint: Boolean
) : OrdinaryImportFixBase<KtArrayAccessExpression>(element, MyFactory) {

    override fun getCallTypeAndReceiver() = element?.let { CallTypeAndReceiver.OPERATOR(it.arrayExpression!!) }

    override fun showHint(editor: Editor) = showHint && super.showHint(editor)

    companion object MyFactory : Factory() {
        private fun importName(diagnostic: Diagnostic): Name {
            return when (diagnostic.factory) {
                Errors.NO_GET_METHOD -> OperatorNameConventions.GET
                Errors.NO_SET_METHOD -> OperatorNameConventions.SET
                else -> throw IllegalStateException("Shouldn't be called for other diagnostics")
            }
        }

        override fun createImportAction(diagnostic: Diagnostic): ArrayAccessorImportFix? {
            val factory = diagnostic.factory
            assert(factory == Errors.NO_GET_METHOD || factory == Errors.NO_SET_METHOD)

            val element = diagnostic.psiElement
            if (element is KtArrayAccessExpression && element.arrayExpression != null) {
                return ArrayAccessorImportFix(element, listOf(importName(diagnostic)), true)
            }

            return null
        }
    }
}

internal class DelegateAccessorsImportFix(
        element: KtExpression,
        override val importNames: Collection<Name>,
        private val solveSeveralProblems: Boolean
) : OrdinaryImportFixBase<KtExpression>(element, MyFactory) {

    override fun getCallTypeAndReceiver() = CallTypeAndReceiver.DELEGATE(element)

    override fun createAction(project: Project, editor: Editor, element: KtExpression): KotlinAddImportAction {
        if (solveSeveralProblems) {
            return createGroupedImportsAction(project, editor, element, "Delegate accessors", suggestions)
        }

        return super.createAction(project, editor, element)
    }

    companion object MyFactory : Factory() {
        private fun importNames(diagnostics: Collection<Diagnostic>): Collection<Name> {
            return diagnostics.map {
                val missingMethodSignature = Errors.DELEGATE_SPECIAL_FUNCTION_MISSING.cast(it).a
                if (missingMethodSignature.startsWith(OperatorNameConventions.GET_VALUE.identifier))
                    OperatorNameConventions.GET_VALUE
                else
                    OperatorNameConventions.SET_VALUE
            }.distinct()
        }

        override fun createImportAction(diagnostic: Diagnostic) =
                (diagnostic.psiElement as? KtExpression)?.let {
                    DelegateAccessorsImportFix(it, importNames(listOf(diagnostic)), false)
                }


        override fun createImportActionsForAllProblems(sameTypeDiagnostics: Collection<Diagnostic>): List<DelegateAccessorsImportFix> {
            val element = sameTypeDiagnostics.first().psiElement
            val names = importNames(sameTypeDiagnostics)
            return listOfNotNull((element as? KtExpression)?.let { DelegateAccessorsImportFix(it, names, true) })
        }
    }
}

internal class ComponentsImportFix(
        element: KtExpression,
        override val importNames: Collection<Name>,
        private val solveSeveralProblems: Boolean
) : OrdinaryImportFixBase<KtExpression>(element, MyFactory) {

    override fun getCallTypeAndReceiver() = element?.let { CallTypeAndReceiver.OPERATOR(it) }

    override fun createAction(project: Project, editor: Editor, element: KtExpression): KotlinAddImportAction {
        if (solveSeveralProblems) {
            return createGroupedImportsAction(project, editor, element, "Component functions", suggestions)
        }

        return super.createAction(project, editor, element)
    }

    companion object MyFactory : Factory() {
        private fun importNames(diagnostics: Collection<Diagnostic>) =
                diagnostics.map { Name.identifier(Errors.COMPONENT_FUNCTION_MISSING.cast(it).a.identifier) }

        override fun createImportAction(diagnostic: Diagnostic) =
                (diagnostic.psiElement as? KtExpression)?.let {
                    ComponentsImportFix(it, importNames(listOf(diagnostic)), false)
                }

        override fun createImportActionsForAllProblems(sameTypeDiagnostics: Collection<Diagnostic>): List<ComponentsImportFix> {
            val element = sameTypeDiagnostics.first().psiElement
            val names = importNames(sameTypeDiagnostics)
            val solveSeveralProblems = sameTypeDiagnostics.size > 1
            return listOfNotNull((element as? KtExpression)?.let { ComponentsImportFix(it, names, solveSeveralProblems) })
        }
    }
}

internal class ImportForMismatchingArgumentsFix(
        expression: KtSimpleNameExpression
) : ImportFixBase<KtSimpleNameExpression>(expression, MyFactory) {
    override fun getCallTypeAndReceiver() = element?.let { CallTypeAndReceiver.detect(it) }

    override val importNames = element?.mainReference?.resolvesByNames ?: emptyList()

    override fun elementsToCheckDiagnostics(): Collection<PsiElement> {
        val element = element ?: return emptyList()
        val callExpression = element.parent as? KtCallExpression ?: return emptyList()
        return callExpression.valueArguments +
               callExpression.valueArguments.mapNotNull { it.getArgumentExpression() } +
               callExpression.valueArguments.mapNotNull { it.getArgumentName()?.referenceExpression } +
               listOfNotNull(callExpression.valueArgumentList,
                             callExpression.referenceExpression())
    }

    override fun fillCandidates(
            name: String,
            callTypeAndReceiver: CallTypeAndReceiver<*, *>,
            bindingContext: BindingContext,
            indicesHelper: KotlinIndicesHelper
    ): List<DeclarationDescriptor> {
        val element = element ?: return emptyList()

        if (!Name.isValidIdentifier(name)) return emptyList()
        val identifier = Name.identifier(name)

        val call = element.getParentCall(bindingContext) ?: return emptyList()
        val callElement = call.callElement as? KtExpression ?: return emptyList()
        if (callElement.anyDescendantOfType<PsiErrorElement>()) return emptyList() // incomplete call
        val elementToAnalyze = callElement.getQualifiedExpressionForSelectorOrThis()

        val file = element.containingKtFile
        val resolutionFacade = file.getResolutionFacade()
        val resolutionScope = elementToAnalyze.getResolutionScope(bindingContext, resolutionFacade)

        val imported = resolutionScope.collectFunctions(identifier, NoLookupLocation.FROM_IDE)

        fun filterFunction(descriptor: FunctionDescriptor): Boolean {
            if (!callTypeAndReceiver.callType.descriptorKindFilter.accepts(descriptor)) return false

            if (descriptor.original in imported) return false // already imported

            // check that this function matches all arguments
            val resolutionScopeWithAddedImport = resolutionScope.addImportingScope(ExplicitImportsScope(listOf(descriptor)))
            val dataFlowInfo = bindingContext.getDataFlowInfoBefore(elementToAnalyze)
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

    companion object MyFactory : Factory() {
        override fun createImportAction(diagnostic: Diagnostic): ImportForMismatchingArgumentsFix? {
            //TODO: not only KtCallExpression
            val callExpression = diagnostic.psiElement.getStrictParentOfType<KtCallExpression>() ?: return null
            val nameExpression = callExpression.calleeExpression as? KtNameReferenceExpression ?: return null
            return ImportForMismatchingArgumentsFix(nameExpression)
        }
    }
}

internal object ImportForMissingOperatorFactory : ImportFixBase.Factory() {
    override fun createImportAction(diagnostic: Diagnostic): ImportFixBase<*>? {
        val element = diagnostic.psiElement as? KtExpression ?: return null
        val operatorDescriptor = Errors.OPERATOR_MODIFIER_REQUIRED.cast(diagnostic).a
        val name = operatorDescriptor.name
        when (name) {
            OperatorNameConventions.GET, OperatorNameConventions.SET -> {
                if (element is KtArrayAccessExpression) {
                    return object : ArrayAccessorImportFix(element, listOf(name), false) {
                        override fun getSupportedErrors() = listOf(Errors.OPERATOR_MODIFIER_REQUIRED)
                    }
                }
            }
        }

        return null
    }
}


private fun KotlinIndicesHelper.getClassesByName(expressionForPlatform: KtExpression, name: String) =
        when (TargetPlatformDetector.getPlatform(expressionForPlatform.containingKtFile)) {
            JsPlatform -> getKotlinClasses({ it == name },
                    // Enum entries should be contributes with members import fix
                                           psiFilter = { ktDeclaration -> ktDeclaration !is KtEnumEntry },
                                           kindFilter = { kind -> kind != ClassKind.ENUM_ENTRY })
            JvmPlatform -> getJvmClassesByName(name)
            else -> emptyList()
        }

private fun CallTypeAndReceiver<*, *>.toFilter() = { descriptor: DeclarationDescriptor -> this.callType.descriptorKindFilter.accepts(descriptor) }