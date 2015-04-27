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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.refactoring.JetNameSuggester
import org.jetbrains.kotlin.idea.refactoring.JetNameValidatorImpl
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
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getValueParameterList
import org.jetbrains.kotlin.psi.psiUtil.getValueParameters
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.JetScopeUtils
import java.util.Collections
import kotlin.test.fail

public data class IntroduceParameterDescriptor(
        val originalRange: JetPsiRange,
        val callable: JetNamedDeclaration,
        val callableDescriptor: FunctionDescriptor,
        val newParameterName: String,
        val newParameterTypeText: String,
        val newArgumentValue: JetExpression,
        val withDefaultValue: Boolean,
        val parametersUsages: Map<JetParameter, List<PsiReference>>,
        val occurrencesToReplace: List<JetPsiRange>,
        val parametersToRemove: List<JetParameter> = getParametersToRemove(withDefaultValue, parametersUsages, occurrencesToReplace),
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
                PsiTreeUtil.findCommonParent(it.elements)?.parents()?.any(modifierIsUnnecessary) ?: false
            }) JetValVar.None else JetValVar.Val
        }
        else JetValVar.None
    }
}

fun getParametersToRemove(
        withDefaultValue: Boolean,
        parametersUsages: Map<JetParameter, List<PsiReference>>,
        occurrencesToReplace: List<JetPsiRange>
): List<JetParameter> {
    if (withDefaultValue) return Collections.emptyList()

    val occurrenceRanges = occurrencesToReplace.map { it.getTextRange() }
    return parametersUsages.entrySet()
            .filter {
                it.value.all { paramRef ->
                    occurrenceRanges.any { occurrenceRange -> occurrenceRange.contains(paramRef.getElement().getTextRange()) }
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
                        parametersToRemove.map { parameters.indexOf(it) }.sortDescending().forEach { methodDescriptor.removeParameter(it) }
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
        if (runChangeSignature(callable.getProject(), callableDescriptor, config, callable.analyze(), callable, INTRODUCE_PARAMETER)) {
            occurrencesToReplace.forEach { occurrenceReplacer(it) }
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
                val parents = parent.parents(withItself = false)
                val stopAt = (parent.parents(withItself = false) zip parent.parents(withItself = false).drop(1))
                        .firstOrNull { isObjectOrNonInnerClass(it.first) }
                        ?.second

                (if (stopAt != null) parent.parents(withItself = false).takeWhile { it != stopAt } else parents)
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
        val replacementType = expressionType.approximateWithResolvableType(JetScopeUtils.getResolutionScope(targetParent, context), false)

        val body = when (targetParent) {
                       is JetFunction -> targetParent.getBodyExpression()
                       is JetClass -> targetParent.getBody()
                       else -> null
                   } ?: throw AssertionError("Body element is not found: ${targetParent.getElementTextWithContext()}")
        val nameValidator = JetNameValidatorImpl(body, null, JetNameValidatorImpl.Target.PROPERTIES)
        val suggestedNames = linkedSetOf(*JetNameSuggester.suggestNames(replacementType, nameValidator, "p"))

        val parametersUsages = findInternalParameterUsages(targetParent)

        val psiFactory = JetPsiFactory(project)
        val renderedType = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(replacementType)
        val newParameter = psiFactory.createParameter("${suggestedNames.first()}: $renderedType")

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

                    val addedParameter = if (inplaceIsAvailable) {
                        runWriteAction {
                            val parameterList = targetParent.getValueParameterList()
                                                ?: (targetParent as JetClass).createPrimaryConstructorParameterListIfAbsent()
                            parameterList.addParameter(newParameter)
                        }
                    }
                    else newParameter

                    val originalExpression = JetPsiUtil.safeDeparenthesize(expression)
                    val introduceParameterDescriptor =
                            helper.configure(
                                    IntroduceParameterDescriptor(
                                            originalRange = originalExpression.toRange(),
                                            callable = targetParent,
                                            callableDescriptor = functionDescriptor,
                                            newParameterName = addedParameter.getName()!!,
                                            newParameterTypeText = renderedType,
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
                                                                          addedParameter,
                                                                          replacementType,
                                                                          editor,
                                                                          project)
                        if (introducer.startRefactoring(suggestedNames)) return
                    }

                    KotlinIntroduceParameterDialog(project,
                                                   editor,
                                                   introduceParameterDescriptor,
                                                   suggestedNames.copyToArray(),
                                                   listOf(replacementType) + replacementType.supertypes(),
                                                   helper).show()
                }
        )
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        (KotlinInplaceVariableIntroducer.getActiveInstance(editor) as? KotlinInplaceParameterIntroducer)?.let {
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

private fun findInternalParameterUsages(targetParent: JetNamedDeclaration): Map<JetParameter, List<PsiReference>> {
    return targetParent.getValueParameters()
            .filter { !it.hasValOrVarNode() }
            .map {
                it to DefaultSearchHelper<JetParameter>()
                        .newRequest(UsagesSearchTarget(element = it))
                        .search()
                        .toList()
            }
            .filter { it.second.isNotEmpty() }
            .toMap()
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
                    parametersUsages = findInternalParameterUsages(callable),
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

val INTRODUCE_PARAMETER: String = "Introduce Parameter"
val INTRODUCE_LAMBDA_PARAMETER: String = "Introduce Lambda Parameter"
