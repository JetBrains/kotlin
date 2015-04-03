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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.refactoring.JetNameSuggester
import org.jetbrains.kotlin.idea.refactoring.JetNameValidatorImpl
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.idea.refactoring.introduce.KotlinIntroduceHandlerBase
import org.jetbrains.kotlin.idea.refactoring.introduce.selectElementsWithTargetParent
import org.jetbrains.kotlin.idea.refactoring.introduce.showErrorHintByKey
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.approximateWithResolvableType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getValueParameterList
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.JetScopeUtils
import org.jetbrains.kotlin.types.JetType
import kotlin.test.fail

public data class IntroduceParameterDescriptor(
        val originalExpression: JetExpression,
        val callable: JetNamedDeclaration,
        val callableDescriptor: FunctionDescriptor,
        val addedParameter: JetParameter,
        val parameterType: JetType
) {
    val valVar: JetValVar

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
            if (originalExpression.parents().any(modifierIsUnnecessary)) JetValVar.None else JetValVar.Val
        }
        else JetValVar.None
    }
}

fun IntroduceParameterDescriptor.performRefactoring() {
    runWriteAction {
        JetPsiUtil.deleteElementWithDelimiters(addedParameter)

        val config = object: JetChangeSignatureConfiguration {
            override fun configure(originalDescriptor: JetMethodDescriptor, bindingContext: BindingContext): JetMethodDescriptor {
                return originalDescriptor.modify {
                    val parameterInfo = JetParameterInfo(name = addedParameter.getName()!!,
                                                         type = parameterType,
                                                         defaultValueForParameter = JetPsiUtil.deparenthesize(originalExpression),
                                                         valOrVar = valVar)
                    parameterInfo.currentTypeText = addedParameter.getTypeReference()?.getText() ?: "Any"
                    addParameter(parameterInfo)
                }
            }

            override fun performSilently(affectedFunctions: Collection<PsiElement>): Boolean = true
        }
        if (runChangeSignature(callable.getProject(), callableDescriptor, config, callable.analyze(), callable, INTRODUCE_PARAMETER)) {
            originalExpression.replace(JetPsiFactory(callable).createSimpleName(addedParameter.getName()!!))
        }
    }
}

public class KotlinIntroduceParameterHandler: KotlinIntroduceHandlerBase() {
    public fun invoke(project: Project, editor: Editor, expression: JetExpression, targetParent: JetNamedDeclaration) {
        val psiFactory = JetPsiFactory(project)

        val parameterList = targetParent.getValueParameterList()

        val context = expression.analyze()
        val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, targetParent]
        val functionDescriptor: FunctionDescriptor =
                when (descriptor) {
                    is FunctionDescriptor -> descriptor : FunctionDescriptor
                    is ClassDescriptor -> descriptor.getUnsubstitutedPrimaryConstructor()
                    else -> null
                } ?: throw AssertionError("Unexpected element type: ${JetPsiUtil.getElementTextWithContext(targetParent)}")
        val expressionType = context[BindingContext.EXPRESSION_TYPE, expression] ?: KotlinBuiltIns.getInstance().getAnyType()
        val parameterType = expressionType.approximateWithResolvableType(JetScopeUtils.getResolutionScope(targetParent, context), false)

        val validatorContainer =
                when (targetParent) {
                    is JetFunction -> targetParent.getBodyExpression()
                    is JetClass -> targetParent.getBody()
                    else -> null
                } ?: throw AssertionError("Body element is not found: ${JetPsiUtil.getElementTextWithContext(targetParent)}")
        val nameValidator = JetNameValidatorImpl(validatorContainer, null, JetNameValidatorImpl.Target.PROPERTIES)
        val suggestedNames = linkedSetOf(*JetNameSuggester.suggestNames(parameterType, nameValidator, "p"))

        project.executeCommand(INTRODUCE_PARAMETER) {
            val renderedType = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(parameterType)
            val newParameter = psiFactory.createParameter("${suggestedNames.first()}: $renderedType")

            val addedParameter = runWriteAction {
                val newParameterList =
                        if (parameterList == null) {
                            val klass = targetParent as? JetClass
                            val anchor = klass?.getTypeParameterList() ?: klass?.getNameIdentifier()
                            assert(anchor != null, "Invalid declaration: ${JetPsiUtil.getElementTextWithContext(targetParent)}")

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

            val introduceParameterDescriptor =
                    IntroduceParameterDescriptor(JetPsiUtil.deparenthesize(expression)!!,
                                                 targetParent,
                                                 functionDescriptor,
                                                 addedParameter,
                                                 parameterType)
            if (editor.getSettings().isVariableInplaceRenameEnabled() && !ApplicationManager.getApplication().isUnitTestMode()) {
                with(PsiDocumentManager.getInstance(project)) {
                    commitDocument(editor.getDocument())
                    doPostponedOperationsAndUnblockDocument(editor.getDocument())
                }

                if (!KotlinInplaceParameterIntroducer(introduceParameterDescriptor, editor, project).startRefactoring(suggestedNames)) {
                    introduceParameterDescriptor.performRefactoring()
                }
            }
            else {
                introduceParameterDescriptor.performRefactoring()
            }
        }
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        if (file !is JetFile) return
        selectElementsWithTargetParent(
                operationName = INTRODUCE_PARAMETER,
                editor = editor,
                file = file,
                getContainers = { elements, parent ->
                    parent.parents(withItself = false)
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

val INTRODUCE_PARAMETER: String = JetRefactoringBundle.message("introduce.property")