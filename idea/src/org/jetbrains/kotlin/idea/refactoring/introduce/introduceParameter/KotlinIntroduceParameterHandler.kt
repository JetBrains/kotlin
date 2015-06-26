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
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.introduce.inplace.AbstractInplaceIntroducer
import com.intellij.refactoring.listeners.RefactoringEventListener
import com.intellij.usageView.BaseUsageViewDescriptor
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.getResolutionScope
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.refactoring.runRefactoringWithPostprocessing
import org.jetbrains.kotlin.idea.refactoring.NameValidatorImpl
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.idea.refactoring.introduce.KotlinIntroduceHandlerBase
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinInplaceVariableIntroducer
import org.jetbrains.kotlin.idea.refactoring.introduce.selectElementsWithTargetParent
import org.jetbrains.kotlin.idea.refactoring.introduce.showErrorHint
import org.jetbrains.kotlin.idea.refactoring.introduce.showErrorHintByKey
import org.jetbrains.kotlin.idea.search.usagesSearch.DefaultSearchHelper
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchTarget
import org.jetbrains.kotlin.idea.search.usagesSearch.search
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.psi.patternMatching.JetPsiRange
import org.jetbrains.kotlin.idea.util.psi.patternMatching.JetPsiUnifier
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.JetScopeUtils
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import java.util.*
import kotlin.test.fail

public data class IntroduceParameterDescriptor(
        val originalRange: JetPsiRange,
        val callable: JetNamedDeclaration,
        val callableDescriptor: FunctionDescriptor,
        val newParameterName: String,
        val newParameterTypeText: String,
        val newArgumentValue: JetExpression,
        val withDefaultValue: Boolean,
        val parametersUsages: MultiMap<JetElement, JetElement>,
        val occurrencesToReplace: List<JetPsiRange>,
        val parametersToRemove: List<JetElement> = getParametersToRemove(withDefaultValue, parametersUsages, occurrencesToReplace),
        val occurrenceReplacer: IntroduceParameterDescriptor.(JetPsiRange) -> Unit = {}
) {
    val originalOccurrence: JetPsiRange
        get() = occurrencesToReplace.first { it.getTextRange().intersects(originalRange.getTextRange()) }
    val valVar: JetValVar

    init {
        valVar = if (callable is JetClass) {
            val modifierIsUnnecessary: (PsiElement) -> Boolean = {
                when {
                    it.getParent() != callable.getBody() ->
                        false
                    it is JetClassInitializer ->
                        true
                    it is JetProperty && it.getInitializer()?.getTextRange()?.intersects(originalRange.getTextRange()) ?: false ->
                        true
                    else ->
                        false
                }
            }
            if (occurrencesToReplace.all {
                PsiTreeUtil.findCommonParent(it.elements)?.parentsWithSelf?.any(modifierIsUnnecessary) ?: false
            }) JetValVar.None else JetValVar.Val
        }
        else JetValVar.None
    }
}

fun getParametersToRemove(
        withDefaultValue: Boolean,
        parametersUsages: MultiMap<JetElement, JetElement>,
        occurrencesToReplace: List<JetPsiRange>
): List<JetElement> {
    if (withDefaultValue) return Collections.emptyList()

    val occurrenceRanges = occurrencesToReplace.map { it.getTextRange() }
    return parametersUsages.entrySet()
            .filter {
                it.value.all { paramUsage ->
                    occurrenceRanges.any { occurrenceRange -> occurrenceRange.contains(paramUsage.getTextRange()) }
                }
            }
            .map { it.key }
}

fun IntroduceParameterDescriptor.performRefactoring() {
    runWriteAction {
        val config = object : JetChangeSignatureConfiguration {
            override fun configure(originalDescriptor: JetMethodDescriptor, bindingContext: BindingContext): JetMethodDescriptor {
                return originalDescriptor.modify { methodDescriptor ->
                    if (!withDefaultValue) {
                        val parameters = callable.getValueParameters()
                        val withReceiver = methodDescriptor.receiver != null
                        parametersToRemove
                                .map {
                                    if (it is JetParameter) {
                                        parameters.indexOf(it) + if (withReceiver) 1 else 0
                                    } else 0
                                }
                                .sortDescending()
                                .forEach { methodDescriptor.removeParameter(it) }
                    }

                    val parameterInfo = JetParameterInfo(functionDescriptor = callableDescriptor,
                                                         name = newParameterName,
                                                         defaultValueForCall = if (withDefaultValue) null else newArgumentValue,
                                                         defaultValueForParameter = if (withDefaultValue) newArgumentValue else null,
                                                         valOrVar = valVar)
                    parameterInfo.currentTypeText = newParameterTypeText
                    methodDescriptor.addParameter(parameterInfo)
                }
            }

            override fun performSilently(affectedFunctions: Collection<PsiElement>): Boolean = true
        }

        val project = callable.getProject();
        val changeSignature = { runChangeSignature(project, callableDescriptor, config, callable.analyze(), callable, INTRODUCE_PARAMETER) }
        changeSignature.runRefactoringWithPostprocessing(project, "refactoring.changeSignature") {
            try {
                occurrencesToReplace.forEach { occurrenceReplacer(it) }
            }
            finally {
                project.getMessageBus()
                        .syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
                        .refactoringDone(INTRODUCE_PARAMETER_REFACTORING_ID, null)
            }
        }
    }
}

private fun isObjectOrNonInnerClass(e: PsiElement): Boolean = e is JetObjectDeclaration || (e is JetClass && !e.isInner())

fun selectNewParameterContext(
        editor: Editor,
        file: PsiFile,
        continuation: (elements: List<PsiElement>, targetParent: PsiElement) -> Unit
) {
    selectElementsWithTargetParent(
            operationName = INTRODUCE_PARAMETER,
            editor = editor,
            file = file,
            getContainers = { elements, parent ->
                val parents = parent.parents
                val stopAt = (parent.parents zip parent.parents.drop(1))
                        .firstOrNull { isObjectOrNonInnerClass(it.first) }
                        ?.second

                (if (stopAt != null) parent.parents.takeWhile { it != stopAt } else parents)
                        .filter {
                            ((it is JetClass && !it.isInterface() && it !is JetEnumEntry) || it is JetNamedFunction || it is JetSecondaryConstructor) &&
                            ((it as JetNamedDeclaration).getValueParameterList() != null || it.getNameIdentifier() != null)
                        }
                        .toList()
            },
            continuation = continuation
    )
}

public trait KotlinIntroduceParameterHelper {
    object Default: KotlinIntroduceParameterHelper

    fun configure(descriptor: IntroduceParameterDescriptor): IntroduceParameterDescriptor = descriptor
}

public open class KotlinIntroduceParameterHandler(
        val helper: KotlinIntroduceParameterHelper = KotlinIntroduceParameterHelper.Default
): KotlinIntroduceHandlerBase() {
    open fun invoke(project: Project, editor: Editor, expression: JetExpression, targetParent: JetNamedDeclaration) {
        val context = expression.analyze()

        val expressionType = context.getType(expression)
        if (expressionType.isUnit() || expressionType.isNothing()) {
            val message = JetRefactoringBundle.message(
                    "cannot.introduce.parameter.of.0.type",
                    IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(expressionType)
            )
            showErrorHint(project, editor, message, INTRODUCE_PARAMETER)
            return
        }

        val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, targetParent]
        val functionDescriptor: FunctionDescriptor =
                when (descriptor) {
                    is FunctionDescriptor -> descriptor : FunctionDescriptor
                    is ClassDescriptor -> descriptor.getUnsubstitutedPrimaryConstructor()
                    else -> null
                } ?: throw AssertionError("Unexpected element type: ${targetParent.getElementTextWithContext()}")
        val replacementType = expressionType.approximateWithResolvableType(targetParent.getResolutionScope(context, targetParent.getResolutionFacade()), false)

        val body = when (targetParent) {
                       is JetFunction -> targetParent.getBodyExpression()
                       is JetClass -> targetParent.getBody()
                       else -> null
                   } ?: throw AssertionError("Body element is not found: ${targetParent.getElementTextWithContext()}")
        val nameValidator = NameValidatorImpl(body, null, NameValidatorImpl.Target.PROPERTIES)
        val suggestedNames = linkedSetOf(*KotlinNameSuggester.suggestNamesByType(replacementType, nameValidator, "p"))

        val parametersUsages = findInternalUsagesOfParametersAndReceiver(targetParent, functionDescriptor)

        val forbiddenRanges =
                if (targetParent is JetClass) {
                    targetParent.getDeclarations().filter { isObjectOrNonInnerClass(it) }.map { it.getTextRange() }
                }
                else {
                    Collections.emptyList()
                }
        val occurrencesToReplace = expression.toRange()
                .match(body, JetPsiUnifier.DEFAULT)
                .filterNot {
                    val textRange = it.range.getTextRange()
                    forbiddenRanges.any { it.intersects(textRange) }
                }
                .map {
                    val matchedElement = it.range.elements.singleOrNull()
                    when (matchedElement) {
                        is JetExpression -> matchedElement
                        is JetStringTemplateEntryWithExpression -> matchedElement.getExpression()
                        else -> null
                    } as? JetExpression
                }
                .filterNotNull()
                .map { it.toRange() }

        project.executeCommand(
                INTRODUCE_PARAMETER,
                null,
                fun() {
                    val isTestMode = ApplicationManager.getApplication().isUnitTestMode()
                    val inplaceIsAvailable = editor.getSettings().isVariableInplaceRenameEnabled() && !isTestMode

                    val originalExpression = JetPsiUtil.safeDeparenthesize(expression)
                    val psiFactory = JetPsiFactory(project)
                    val introduceParameterDescriptor =
                            helper.configure(
                                    IntroduceParameterDescriptor(
                                            originalRange = originalExpression.toRange(),
                                            callable = targetParent,
                                            callableDescriptor = functionDescriptor,
                                            newParameterName = suggestedNames.first(),
                                            newParameterTypeText = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(replacementType),
                                            newArgumentValue = originalExpression,
                                            withDefaultValue = false,
                                            parametersUsages = parametersUsages,
                                            occurrencesToReplace = occurrencesToReplace,
                                            occurrenceReplacer = {
                                                it.elements.single().replace(psiFactory.createExpression(newParameterName))
                                            }
                                    )
                            )
                    if (isTestMode) {
                        introduceParameterDescriptor.performRefactoring()
                        return
                    }

                    if (inplaceIsAvailable) {
                        with(PsiDocumentManager.getInstance(project)) {
                            commitDocument(editor.getDocument())
                            doPostponedOperationsAndUnblockDocument(editor.getDocument())
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

        if (file !is JetFile) return
        selectNewParameterContext(editor, file) { elements, targetParent ->
            val expression = ((elements.singleOrNull() as? JetBlockExpression)?.getStatements() ?: elements).singleOrNull()
            if (expression is JetExpression) {
                invoke(project, editor, expression, targetParent as JetNamedDeclaration)
            }
            else {
                showErrorHintByKey(project, editor, "cannot.refactor.no.expression", INTRODUCE_PARAMETER)
            }
        }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        fail("$INTRODUCE_PARAMETER can only be invoked from editor")
    }
}

private fun findInternalUsagesOfParametersAndReceiver(
        targetParent: JetNamedDeclaration,
        targetDescriptor: FunctionDescriptor
): MultiMap<JetElement, JetElement> {
    val usages = MultiMap<JetElement, JetElement>()
    targetParent.getValueParameters()
            .filter { !it.hasValOrVar() }
            .forEach {
                val paramUsages = DefaultSearchHelper<JetParameter>()
                        .newRequest(UsagesSearchTarget(element = it))
                        .search()
                        .map { it.getElement() as JetElement }
                if (paramUsages.isNotEmpty()) {
                    usages.put(it, paramUsages)
                }
            }
    val receiverTypeRef = (targetParent as? JetFunction)?.getReceiverTypeReference()
    if (receiverTypeRef != null) {
        targetParent.acceptChildren(
                object : JetTreeVisitorVoid() {
                    override fun visitThisExpression(expression: JetThisExpression) {
                        super.visitThisExpression(expression)

                        if (expression.getInstanceReference().getReference()?.resolve() == targetDescriptor) {
                            usages.putValue(receiverTypeRef, expression)
                        }
                    }

                    override fun visitJetElement(element: JetElement) {
                        super.visitJetElement(element)

                        val bindingContext = element.analyze()
                        val resolvedCall = element.getResolvedCall(bindingContext) ?: return

                        if ((resolvedCall.getExtensionReceiver() as? ThisReceiver)?.getDeclarationDescriptor() == targetDescriptor ||
                            (resolvedCall.getDispatchReceiver() as? ThisReceiver)?.getDeclarationDescriptor() == targetDescriptor) {
                            usages.putValue(receiverTypeRef, resolvedCall.getCall().getCallElement())
                        }
                    }
                }
        )
    }
    return usages
}

trait KotlinIntroduceLambdaParameterHelper: KotlinIntroduceParameterHelper {
    object Default: KotlinIntroduceLambdaParameterHelper

    fun configureExtractLambda(descriptor: ExtractableCodeDescriptor): ExtractableCodeDescriptor = descriptor
}

public open class KotlinIntroduceLambdaParameterHandler(
        helper: KotlinIntroduceLambdaParameterHelper = KotlinIntroduceLambdaParameterHelper.Default
): KotlinIntroduceParameterHandler(helper) {
    val extractLambdaHelper = object: ExtractionEngineHelper(INTRODUCE_LAMBDA_PARAMETER) {
        private fun createDialog(
                project: Project,
                editor: Editor,
                lambdaExtractionDescriptor: ExtractableCodeDescriptor
        ): KotlinIntroduceParameterDialog {
            val callable = lambdaExtractionDescriptor.extractionData.targetSibling as JetNamedDeclaration
            val descriptor = callable.resolveToDescriptor()
            val callableDescriptor: FunctionDescriptor =
                    when (descriptor) {
                        is FunctionDescriptor -> descriptor : FunctionDescriptor
                        is ClassDescriptor -> descriptor.getUnsubstitutedPrimaryConstructor()
                        else -> null
                    } ?: throw AssertionError("Unexpected element type: ${callable.getElementTextWithContext()}")
            val originalRange = lambdaExtractionDescriptor.extractionData.originalRange
            val introduceParameterDescriptor = IntroduceParameterDescriptor(
                    originalRange = originalRange,
                    callable = callable,
                    callableDescriptor = callableDescriptor,
                    newParameterName = "", // to be chosen in the dialog
                    newParameterTypeText = "", // to be chosen in the dialog
                    newArgumentValue = JetPsiFactory(project).createExpression("{}"), // substituted later
                    withDefaultValue = false,
                    parametersUsages = findInternalUsagesOfParametersAndReceiver(callable, callableDescriptor),
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

            val dialog = createDialog(project, editor, lambdaExtractionDescriptor)
            if (ApplicationManager.getApplication()!!.isUnitTestMode()) {
                dialog.performRefactoring()
            }
            else {
                dialog.show()
            }
        }
    }

    override fun invoke(project: Project, editor: Editor, expression: JetExpression, targetParent: JetNamedDeclaration) {
        val duplicateContainer =
                when (targetParent) {
                    is JetFunction -> targetParent.getBodyExpression()
                    is JetClass -> targetParent.getBody()
                    else -> null
                } ?: throw AssertionError("Body element is not found: ${targetParent.getElementTextWithContext()}")
        val extractionData = ExtractionData(expression.getContainingJetFile(),
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
