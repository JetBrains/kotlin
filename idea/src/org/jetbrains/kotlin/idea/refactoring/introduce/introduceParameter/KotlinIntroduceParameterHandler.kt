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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.refactoring.JetNameSuggester
import org.jetbrains.kotlin.idea.refactoring.JetNameValidatorImpl
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.idea.refactoring.introduce.KotlinIntroduceHandlerBase
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
import org.jetbrains.kotlin.idea.util.psi.patternMatching.JetPsiUnifier
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.idea.util.supertypes
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.JetScopeUtils
import org.jetbrains.kotlin.types.JetType
import java.util.Collections
import kotlin.test.fail

public data class IntroduceParameterDescriptor(
        val originalExpression: JetExpression,
        val callable: JetNamedDeclaration,
        val callableDescriptor: FunctionDescriptor,
        val addedParameter: JetParameter,
        val parameterType: JetType,
        val withDefaultValue: Boolean,
        val parametersUsages: Map<JetParameter, List<PsiReference>>,
        val occurrencesToReplace: List<JetExpression>
) {
    val originalOccurrence: JetExpression
        get() = occurrencesToReplace.first { it.isAncestor(originalExpression) }
    val valVar: JetValVar
    val parametersToRemove: List<JetParameter>

    init {
        valVar = if (callable is JetClass) {
            val modifierIsUnnecessary: (PsiElement) -> Boolean = {
                when {
                    it.getParent() != callable.getBody() ->
                        false
                    it is JetClassInitializer ->
                        true
                    it is JetProperty && it.getInitializer()?.getTextRange()?.intersects(originalExpression.getTextRange()) ?: false ->
                        true
                    else ->
                        false
                }
            }
            if (occurrencesToReplace.all { it.parents().any(modifierIsUnnecessary) }) JetValVar.None else JetValVar.Val
        }
        else JetValVar.None
        
        parametersToRemove =
                if (withDefaultValue) Collections.emptyList()
                else {
                    val occurrenceRanges = occurrencesToReplace.map { it.getTextRange() }
                    parametersUsages.entrySet()
                            .filter {
                                it.value.all { paramRef ->
                                    occurrenceRanges.any { occurrenceRange -> occurrenceRange.contains(paramRef.getElement().getTextRange()) }
                                }
                            }
                            .map { it.key }
                }
    }
}

fun IntroduceParameterDescriptor.performRefactoring(parametersToRemove: List<JetParameter> = this.parametersToRemove) {
    runWriteAction {
        JetPsiUtil.deleteElementWithDelimiters(addedParameter)
        
        val config = object: JetChangeSignatureConfiguration {
            override fun configure(originalDescriptor: JetMethodDescriptor, bindingContext: BindingContext): JetMethodDescriptor {
                return originalDescriptor.modify {
                    val parameters = callable.getValueParameters()
                    parametersToRemove.map { parameters.indexOf(it) }.sortDescending().forEach { removeParameter(it) }
                    
                    val parameterInfo = JetParameterInfo(name = addedParameter.getName()!!,
                                                         type = parameterType,
                                                         defaultValueForCall = if (withDefaultValue) "" else originalExpression.getText(),
                                                         defaultValueForParameter = if (withDefaultValue) originalExpression else null,
                                                         valOrVar = valVar)
                    parameterInfo.currentTypeText = addedParameter.getTypeReference()?.getText() ?: "Any"
                    addParameter(parameterInfo)
                }
            }

            override fun performSilently(affectedFunctions: Collection<PsiElement>): Boolean = true
        }
        if (runChangeSignature(callable.getProject(), callableDescriptor, config, callable.analyze(), callable, INTRODUCE_PARAMETER)) {
            val paramRef = JetPsiFactory(callable).createSimpleName(addedParameter.getName()!!)
            occurrencesToReplace.forEach { it.replace(paramRef) }
        }
    }
}

public open class KotlinIntroduceParameterHandler: KotlinIntroduceHandlerBase() {
    open fun configure(descriptor: IntroduceParameterDescriptor): IntroduceParameterDescriptor = descriptor

    private fun isObjectOrNonInnerClass(e: PsiElement): Boolean =
            e is JetObjectDeclaration || (e is JetClass && !e.isInner())

    fun invoke(project: Project, editor: Editor, expression: JetExpression, targetParent: JetNamedDeclaration) {
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

        val parameterList = targetParent.getValueParameterList()

        val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, targetParent]
        val functionDescriptor: FunctionDescriptor =
                when (descriptor) {
                    is FunctionDescriptor -> descriptor : FunctionDescriptor
                    is ClassDescriptor -> descriptor.getUnsubstitutedPrimaryConstructor()
                    else -> null
                } ?: throw AssertionError("Unexpected element type: ${targetParent.getElementTextWithContext()}")
        val parameterType = expressionType.approximateWithResolvableType(JetScopeUtils.getResolutionScope(targetParent, context), false)

        val body = when (targetParent) {
                       is JetFunction -> targetParent.getBodyExpression()
                       is JetClass -> targetParent.getBody()
                       else -> null
                   } ?: throw AssertionError("Body element is not found: ${targetParent.getElementTextWithContext()}")
        val nameValidator = JetNameValidatorImpl(body, null, JetNameValidatorImpl.Target.PROPERTIES)
        val suggestedNames = linkedSetOf(*JetNameSuggester.suggestNames(parameterType, nameValidator, "p"))

        val parametersUsages = targetParent.getValueParameters()
                .filter { !it.hasValOrVarNode() }
                .map {
                    it to DefaultSearchHelper<JetParameter>()
                            .newRequest(UsagesSearchTarget(element = it))
                            .search()
                            .toList()
                }
                .filter { it.second.isNotEmpty() }
                .toMap()

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

        project.executeCommand(
                INTRODUCE_PARAMETER,
                null,
                fun() {
                    val psiFactory = JetPsiFactory(project)
                    
                    val renderedType = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(parameterType)
                    val newParameter = psiFactory.createParameter("${suggestedNames.first()}: $renderedType")

                    val isTestMode = ApplicationManager.getApplication().isUnitTestMode()
                    val inplaceIsAvailable = editor.getSettings().isVariableInplaceRenameEnabled() && !isTestMode

                    val addedParameter = if (inplaceIsAvailable) {
                        runWriteAction {
                            val newParameterList =
                                    if (parameterList == null) {
                                        val klass = targetParent as? JetClass
                                        val anchor = klass?.getTypeParameterList() ?: klass?.getNameIdentifier()
                                        assert(anchor != null, "Invalid declaration: ${targetParent.getElementTextWithContext()}")

                                        val constructor = targetParent.addAfter(psiFactory.createPrimaryConstructor(), anchor) as JetPrimaryConstructor
                                        constructor.getValueParameterList()!!
                                    }
                                    else parameterList

                            val lastParameter = newParameterList.getChildren().lastOrNull { it is JetParameter } as? JetParameter
                            if (lastParameter != null) {
                                val comma = newParameterList.addAfter(psiFactory.createComma(), lastParameter)
                                newParameterList.addAfter(newParameter, comma) as JetParameter
                            }
                            else {
                                val singleParameterList = psiFactory.createParameterList("(${newParameter.getText()})")
                                (newParameterList.replace(singleParameterList) as JetParameterList).getParameters().first()
                            }
                        }
                    }
                    else newParameter

                    val introduceParameterDescriptor =
                            configure(IntroduceParameterDescriptor(JetPsiUtil.deparenthesize(expression)!!,
                                                                   targetParent,
                                                                   functionDescriptor,
                                                                   addedParameter,
                                                                   parameterType,
                                                                   false,
                                                                   parametersUsages,
                                                                   occurrencesToReplace))
                    if (isTestMode) {
                        introduceParameterDescriptor.performRefactoring()
                        return
                    }

                    if (inplaceIsAvailable) {
                        with(PsiDocumentManager.getInstance(project)) {
                            commitDocument(editor.getDocument())
                            doPostponedOperationsAndUnblockDocument(editor.getDocument())
                        }

                        if (KotlinInplaceParameterIntroducer(introduceParameterDescriptor, editor, project)
                                .startRefactoring(suggestedNames)) return
                    }

                    KotlinIntroduceParameterDialog(project,
                                                   introduceParameterDescriptor,
                                                   suggestedNames.copyToArray(),
                                                   listOf(parameterType) + parameterType.supertypes()).show()
                }
        )
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        (KotlinInplaceVariableIntroducer.getActiveInstance(editor) as? KotlinInplaceParameterIntroducer)?.let {
            it.switchToDialogUI()
            return
        }

        if (file !is JetFile) return
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
                                ((it is JetClass && !it.isTrait() && it !is JetEnumEntry) || it is JetNamedFunction || it is JetSecondaryConstructor) &&
                                ((it as JetNamedDeclaration).getValueParameterList() != null || it.getNameIdentifier() != null)
                            }
                            .toList()
                },
                continuation = { elements, targetParent ->
                    val expression = ((elements.singleOrNull() as? JetBlockExpression)?.getStatements() ?: elements).singleOrNull()
                    if (expression is JetExpression) {
                        invoke(project, editor, expression, targetParent as JetNamedDeclaration)
                    }
                    else {
                        showErrorHintByKey(project, editor, "cannot.refactor.no.expression", INTRODUCE_PARAMETER)
                    }
                }
        )
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        fail("$INTRODUCE_PARAMETER can only be invoked from editor")
    }
}

val INTRODUCE_PARAMETER: String = JetRefactoringBundle.message("introduce.parameter")
