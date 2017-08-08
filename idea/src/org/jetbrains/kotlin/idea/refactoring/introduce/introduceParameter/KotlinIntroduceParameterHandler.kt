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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceParameter

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.introduce.inplace.AbstractInplaceIntroducer
import com.intellij.refactoring.listeners.RefactoringEventListener
import com.intellij.util.SmartList
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.idea.refactoring.introduce.*
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.idea.refactoring.removeTemplateEntryBracesIfPossible
import org.jetbrains.kotlin.idea.refactoring.runRefactoringWithPostprocessing
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.approximateWithResolvableType
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiRange
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiUnifier
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

data class IntroduceParameterDescriptor(
        val originalRange: KotlinPsiRange,
        val callable: KtNamedDeclaration,
        val callableDescriptor: FunctionDescriptor,
        val newParameterName: String,
        val newParameterTypeText: String,
        val argumentValue: KtExpression,
        val withDefaultValue: Boolean,
        val parametersUsages: MultiMap<KtElement, KtElement>,
        val occurrencesToReplace: List<KotlinPsiRange>,
        val parametersToRemove: List<KtElement> = getParametersToRemove(withDefaultValue, parametersUsages, occurrencesToReplace),
        val occurrenceReplacer: IntroduceParameterDescriptor.(KotlinPsiRange) -> Unit = {}
) {
    val newArgumentValue: KtExpression by lazy {
        if (argumentValue.mustBeParenthesizedInInitializerPosition()) {
            KtPsiFactory(callable).createExpressionByPattern("($0)", argumentValue)
        }
        else {
            argumentValue
        }
    }

    val originalOccurrence: KotlinPsiRange
        get() = occurrencesToReplace.first { it.getTextRange().intersects(originalRange.getTextRange()) }
    val valVar: KotlinValVar

    init {
        valVar = if (callable is KtClass) {
            val modifierIsUnnecessary: (PsiElement) -> Boolean = {
                when {
                    it.parent != callable.getBody() ->
                        false
                    it is KtAnonymousInitializer ->
                        true
                    it is KtProperty && it.initializer?.textRange?.intersects(originalRange.getTextRange()) ?: false ->
                        true
                    else ->
                        false
                }
            }
            if (occurrencesToReplace.all {
                PsiTreeUtil.findCommonParent(it.elements)?.parentsWithSelf?.any(modifierIsUnnecessary) ?: false
            }) KotlinValVar.None else KotlinValVar.Val
        }
        else KotlinValVar.None
    }
}

fun getParametersToRemove(
        withDefaultValue: Boolean,
        parametersUsages: MultiMap<KtElement, KtElement>,
        occurrencesToReplace: List<KotlinPsiRange>
): List<KtElement> {
    if (withDefaultValue) return Collections.emptyList()

    val occurrenceRanges = occurrencesToReplace.map { it.getTextRange() }
    return parametersUsages.entrySet()
            .filter {
                it.value.all { paramUsage ->
                    occurrenceRanges.any { occurrenceRange -> occurrenceRange.contains(paramUsage.textRange) }
                }
            }
            .map { it.key }
}

fun IntroduceParameterDescriptor.performRefactoring() {
    val config = object : KotlinChangeSignatureConfiguration {
        override fun configure(originalDescriptor: KotlinMethodDescriptor): KotlinMethodDescriptor {
            return originalDescriptor.modify { methodDescriptor ->
                if (!withDefaultValue) {
                    val parameters = callable.getValueParameters()
                    val withReceiver = methodDescriptor.receiver != null
                    parametersToRemove
                            .map {
                                if (it is KtParameter) {
                                    parameters.indexOf(it) + if (withReceiver) 1 else 0
                                } else 0
                            }
                            .sortedDescending()
                            .forEach { methodDescriptor.removeParameter(it) }
                }

                val defaultValue = if (newArgumentValue is KtProperty) (newArgumentValue as KtProperty).initializer else newArgumentValue
                val parameterInfo = KotlinParameterInfo(callableDescriptor = callableDescriptor,
                                                        name = newParameterName,
                                                        defaultValueForCall = if (withDefaultValue) null else defaultValue,
                                                        defaultValueForParameter = if (withDefaultValue) defaultValue else null,
                                                        valOrVar = valVar)
                parameterInfo.currentTypeInfo = KotlinTypeInfo(false, null, newParameterTypeText)
                methodDescriptor.addParameter(parameterInfo)
            }
        }

        override fun performSilently(affectedFunctions: Collection<PsiElement>): Boolean = true
    }

    val project = callable.project
    val changeSignature = { runChangeSignature(project, callableDescriptor, config, callable, INTRODUCE_PARAMETER) }

    changeSignature.runRefactoringWithPostprocessing(project, "refactoring.changeSignature") {
        try {
            occurrencesToReplace.forEach { occurrenceReplacer(it) }
        }
        finally {
            project.messageBus
                    .syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
                    .refactoringDone(INTRODUCE_PARAMETER_REFACTORING_ID, null)
        }
    }
}

fun selectNewParameterContext(
        editor: Editor,
        file: KtFile,
        continuation: (elements: List<PsiElement>, targetParent: PsiElement) -> Unit
) {
    selectElementsWithTargetParent(
            operationName = INTRODUCE_PARAMETER,
            editor = editor,
            file = file,
            title = "Introduce parameter to declaration",
            elementKinds = listOf(CodeInsightUtils.ElementKind.EXPRESSION),
            getContainers = { _, parent ->
                val parents = parent.parents
                val stopAt = (parent.parents.zip(parent.parents.drop(1)))
                        .firstOrNull { isObjectOrNonInnerClass(it.first) }
                        ?.second

                (if (stopAt != null) parent.parents.takeWhile { it != stopAt } else parents)
                        .filter {
                            ((it is KtClass && !it.isInterface() && it !is KtEnumEntry) || it is KtNamedFunction || it is KtSecondaryConstructor) &&
                            ((it as KtNamedDeclaration).getValueParameterList() != null || it.nameIdentifier != null)
                        }
                        .toList()
            },
            continuation = continuation
    )
}

interface KotlinIntroduceParameterHelper {
    object Default: KotlinIntroduceParameterHelper

    fun configure(descriptor: IntroduceParameterDescriptor): IntroduceParameterDescriptor = descriptor
}

open class KotlinIntroduceParameterHandler(
        val helper: KotlinIntroduceParameterHelper = KotlinIntroduceParameterHelper.Default
): RefactoringActionHandler {
    open fun invoke(project: Project, editor: Editor, expression: KtExpression, targetParent: KtNamedDeclaration) {
        val physicalExpression = expression.substringContextOrThis
        if (physicalExpression is KtProperty && physicalExpression.isLocal && physicalExpression.nameIdentifier == null) {
            showErrorHintByKey(project, editor, "cannot.refactor.no.expression", INTRODUCE_PARAMETER)
            return
        }

        val context = physicalExpression.analyze()

        val expressionType = if (physicalExpression is KtProperty && physicalExpression.isLocal) {
            context[BindingContext.VARIABLE, physicalExpression]?.type
        }
        else {
            expression.extractableSubstringInfo?.type ?: context.getType(physicalExpression)
        }

        if (expressionType == null) {
            showErrorHint(project, editor, "Expression has no type", INTRODUCE_PARAMETER)
            return
        }

        if (expressionType.isUnit() || expressionType.isNothing()) {
            val message = KotlinRefactoringBundle.message(
                    "cannot.introduce.parameter.of.0.type",
                    IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(expressionType)
            )
            showErrorHint(project, editor, message, INTRODUCE_PARAMETER)
            return
        }

        val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, targetParent]
        val functionDescriptor = descriptor.toFunctionDescriptor(targetParent)
        val replacementType = expressionType.approximateWithResolvableType(targetParent.getResolutionScope(context, targetParent.getResolutionFacade()), false)

        val body = when (targetParent) {
                       is KtFunction -> targetParent.bodyExpression
                       is KtClass -> targetParent.getBody()
                       else -> null
                   } ?: throw AssertionError("Body element is not found: ${targetParent.getElementTextWithContext()}")
        val nameValidator = NewDeclarationNameValidator(body, sequenceOf(body), NewDeclarationNameValidator.Target.VARIABLES)

        val suggestedNames = SmartList<String>().apply {
            if (physicalExpression is KtProperty && !ApplicationManager.getApplication().isUnitTestMode) {
                addIfNotNull(physicalExpression.name)
            }
            addAll(KotlinNameSuggester.suggestNamesByType(replacementType, nameValidator, "p"))
        }

        val parametersUsages = findInternalUsagesOfParametersAndReceiver(targetParent, functionDescriptor) ?: return

        val forbiddenRanges = (targetParent as? KtClass)?.declarations?.filter(::isObjectOrNonInnerClass)?.map { it.textRange }
                              ?: Collections.emptyList()

        val occurrencesToReplace = if (expression is KtProperty) {
            ReferencesSearch.search(expression).mapNotNullTo(SmartList(expression.toRange())) { it.element?.toRange() }
        }
        else {
            expression.toRange()
                    .match(body, KotlinPsiUnifier.DEFAULT)
                    .filterNot {
                        val textRange = it.range.getPhysicalTextRange()
                        forbiddenRanges.any { it.intersects(textRange) }
                    }
                    .mapNotNull {
                        val matchedElement = it.range.elements.singleOrNull()
                        val matchedExpr = when (matchedElement) {
                            is KtExpression -> matchedElement
                            is KtStringTemplateEntryWithExpression -> matchedElement.expression
                            else -> null
                        } as? KtExpression
                        matchedExpr?.toRange()
                    }
        }

        project.executeCommand(
                INTRODUCE_PARAMETER,
                null,
                fun() {
                    val isTestMode = ApplicationManager.getApplication().isUnitTestMode
                    val haveLambdaArgumentsToReplace = occurrencesToReplace.any {
                        it.elements.any { it is KtLambdaExpression && it.parent is KtLambdaArgument }
                    }
                    val inplaceIsAvailable = editor.settings.isVariableInplaceRenameEnabled
                                             && !isTestMode
                                             && !haveLambdaArgumentsToReplace
                                             && expression.extractableSubstringInfo == null
                                             && !expression.mustBeParenthesizedInInitializerPosition()

                    val originalExpression = KtPsiUtil.safeDeparenthesize(expression)
                    val psiFactory = KtPsiFactory(project)
                    val introduceParameterDescriptor =
                            helper.configure(
                                    IntroduceParameterDescriptor(
                                            originalRange = originalExpression.toRange(),
                                            callable = targetParent,
                                            callableDescriptor = functionDescriptor,
                                            newParameterName = suggestedNames.first().quoteIfNeeded(),
                                            newParameterTypeText = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(replacementType),
                                            argumentValue = originalExpression,
                                            withDefaultValue = false,
                                            parametersUsages = parametersUsages,
                                            occurrencesToReplace = occurrencesToReplace,
                                            occurrenceReplacer = replacer@ {
                                                val expressionToReplace = it.elements.single() as KtExpression
                                                val replacingExpression = psiFactory.createExpression(newParameterName)
                                                val substringInfo = expressionToReplace.extractableSubstringInfo
                                                val result = when {
                                                    expressionToReplace is KtProperty -> return@replacer expressionToReplace.delete()
                                                    expressionToReplace.isLambdaOutsideParentheses() -> {
                                                        expressionToReplace
                                                                .getStrictParentOfType<KtLambdaArgument>()!!
                                                                .moveInsideParenthesesAndReplaceWith(replacingExpression, context)
                                                    }
                                                    substringInfo != null -> substringInfo.replaceWith(replacingExpression)
                                                    else -> expressionToReplace.replaced(replacingExpression)
                                                }
                                                result.removeTemplateEntryBracesIfPossible()
                                            }
                                    )
                            )
                    if (isTestMode) {
                        introduceParameterDescriptor.performRefactoring()
                        return
                    }

                    if (inplaceIsAvailable) {
                        with(PsiDocumentManager.getInstance(project)) {
                            commitDocument(editor.document)
                            doPostponedOperationsAndUnblockDocument(editor.document)
                        }

                        val introducer = KotlinInplaceParameterIntroducer(introduceParameterDescriptor,
                                                                          replacementType,
                                                                          suggestedNames.toTypedArray(),
                                                                          project,
                                                                          editor)
                        if (introducer.startInplaceIntroduceTemplate()) return
                    }

                    KotlinIntroduceParameterDialog(project,
                                                   editor,
                                                   introduceParameterDescriptor,
                                                   suggestedNames.toTypedArray(),
                                                   listOf(replacementType) + replacementType.supertypes(),
                                                   helper).show()
                }
        )
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        (AbstractInplaceIntroducer.getActiveIntroducer(editor) as? KotlinInplaceParameterIntroducer)?.let {
            it.switchToDialogUI()
            return
        }

        if (file !is KtFile) return
        selectNewParameterContext(editor, file) { elements, targetParent ->
            val expression = ((elements.singleOrNull() as? KtBlockExpression)?.statements ?: elements).singleOrNull()
            if (expression is KtExpression) {
                invoke(project, editor, expression, targetParent as KtNamedDeclaration)
            }
            else {
                showErrorHintByKey(project, editor, "cannot.refactor.no.expression", INTRODUCE_PARAMETER)
            }
        }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        throw AssertionError("$INTRODUCE_PARAMETER can only be invoked from editor")
    }
}

private fun DeclarationDescriptor?.toFunctionDescriptor(targetParent: KtNamedDeclaration): FunctionDescriptor {
    val functionDescriptor: FunctionDescriptor? =
            when (this) {
                is FunctionDescriptor -> this
                is ClassDescriptor -> this.unsubstitutedPrimaryConstructor
                else -> null
            }
    return functionDescriptor ?: throw AssertionError("Unexpected element type: ${targetParent.getElementTextWithContext()}")
}

private fun findInternalUsagesOfParametersAndReceiver(
        targetParent: KtNamedDeclaration,
        targetDescriptor: FunctionDescriptor
): MultiMap<KtElement, KtElement>? {
    val usages = MultiMap<KtElement, KtElement>()
    val searchComplete = targetParent.project.runSynchronouslyWithProgress("Searching usages of '${targetParent.name}' parameter", true) {
        runReadAction {
            targetParent.getValueParameters()
                    .filter { !it.hasValOrVar() }
                    .forEach {
                        val paramUsages = ReferencesSearch.search(it).map { it.element as KtElement }
                        if (paramUsages.isNotEmpty()) {
                            usages.put(it, paramUsages)
                        }
                    }
        }
    } != null
    if (!searchComplete) return null
    val receiverTypeRef = (targetParent as? KtFunction)?.receiverTypeReference
    if (receiverTypeRef != null) {
        targetParent.acceptChildren(
                object : KtTreeVisitorVoid() {
                    override fun visitThisExpression(expression: KtThisExpression) {
                        super.visitThisExpression(expression)

                        if (expression.instanceReference.mainReference.resolve() == targetDescriptor) {
                            usages.putValue(receiverTypeRef, expression)
                        }
                    }

                    override fun visitKtElement(element: KtElement) {
                        super.visitKtElement(element)

                        val bindingContext = element.analyze()
                        val resolvedCall = element.getResolvedCall(bindingContext) ?: return

                        if ((resolvedCall.extensionReceiver as? ImplicitReceiver)?.declarationDescriptor == targetDescriptor ||
                            (resolvedCall.dispatchReceiver as? ImplicitReceiver)?.declarationDescriptor == targetDescriptor) {
                            usages.putValue(receiverTypeRef, resolvedCall.call.callElement)
                        }
                    }
                }
        )
    }
    return usages
}

interface KotlinIntroduceLambdaParameterHelper: KotlinIntroduceParameterHelper {
    object Default: KotlinIntroduceLambdaParameterHelper

    fun configureExtractLambda(descriptor: ExtractableCodeDescriptor): ExtractableCodeDescriptor = descriptor
}

open class KotlinIntroduceLambdaParameterHandler(
        helper: KotlinIntroduceLambdaParameterHelper = KotlinIntroduceLambdaParameterHelper.Default
): KotlinIntroduceParameterHandler(helper) {
    private val extractLambdaHelper = object: ExtractionEngineHelper(INTRODUCE_LAMBDA_PARAMETER) {
        private fun createDialog(
                project: Project,
                editor: Editor,
                lambdaExtractionDescriptor: ExtractableCodeDescriptor
        ): KotlinIntroduceParameterDialog? {
            val callable = lambdaExtractionDescriptor.extractionData.targetSibling as KtNamedDeclaration
            val descriptor = callable.resolveToDescriptor()
            val callableDescriptor = descriptor.toFunctionDescriptor(callable)
            val originalRange = lambdaExtractionDescriptor.extractionData.originalRange
            val parametersUsages = findInternalUsagesOfParametersAndReceiver(callable, callableDescriptor) ?: return null
            val introduceParameterDescriptor = IntroduceParameterDescriptor(
                    originalRange = originalRange,
                    callable = callable,
                    callableDescriptor = callableDescriptor,
                    newParameterName = "", // to be chosen in the dialog
                    newParameterTypeText = "", // to be chosen in the dialog
                    argumentValue = KtPsiFactory(project).createExpression("{}"), // substituted later
                    withDefaultValue = false,
                    parametersUsages = parametersUsages,
                    occurrencesToReplace = listOf(originalRange),
                    parametersToRemove = listOf()
            )

            return KotlinIntroduceParameterDialog(project, editor, introduceParameterDescriptor, lambdaExtractionDescriptor, helper)
        }

        override fun configureAndRun(
                project: Project,
                editor: Editor,
                descriptorWithConflicts: ExtractableCodeDescriptorWithConflicts,
                onFinish: (ExtractionResult) -> Unit
        ) {
            val lambdaExtractionDescriptor = helper.configureExtractLambda(descriptorWithConflicts.descriptor)
            if (!ExtractionTarget.FAKE_LAMBDALIKE_FUNCTION.isAvailable(lambdaExtractionDescriptor)) {
                showErrorHint(project, editor, "Can't introduce lambda parameter for this expression", INTRODUCE_LAMBDA_PARAMETER)
                return
            }

            val dialog = createDialog(project, editor, lambdaExtractionDescriptor) ?: return
            if (ApplicationManager.getApplication()!!.isUnitTestMode) {
                dialog.performRefactoring()
            }
            else {
                dialog.show()
            }
        }
    }

    override fun invoke(project: Project, editor: Editor, expression: KtExpression, targetParent: KtNamedDeclaration) {
        val duplicateContainer =
                when (targetParent) {
                    is KtFunction -> targetParent.bodyExpression
                    is KtClass -> targetParent.getBody()
                    else -> null
                } ?: throw AssertionError("Body element is not found: ${targetParent.getElementTextWithContext()}")
        val extractionData = ExtractionData(targetParent.containingKtFile,
                                            expression.toRange(),
                                            targetParent,
                                            duplicateContainer,
                                            ExtractionOptions.DEFAULT)
        ExtractionEngine(extractLambdaHelper).run(editor, extractionData)
    }
}

val INTRODUCE_PARAMETER_REFACTORING_ID: String = "kotlin.refactoring.introduceParameter"

val INTRODUCE_PARAMETER: String = "Introduce Parameter"
val INTRODUCE_LAMBDA_PARAMETER: String = "Introduce Lambda Parameter"
