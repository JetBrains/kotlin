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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.HelpID
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.changeSignature.ChangeSignatureHandler
import com.intellij.refactoring.changeSignature.ChangeSignatureUtil
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

public class JetChangeSignatureHandler : ChangeSignatureHandler {

    override fun findTargetMember(file: PsiFile, editor: Editor) =
            file.findElementAt(editor.getCaretModel().getOffset())?.let { findTargetMember(it) }

    override fun findTargetMember(element: PsiElement) =
            findTargetForRefactoring(element)

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext) {
        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE)

        val element = findTargetMember(file, editor) ?: CommonDataKeys.PSI_ELEMENT.getData(dataContext) ?: return
        val elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset()) ?: return
        if (element !is JetElement) throw AssertionError("This handler must be invoked for Kotlin elements only: ${element.getText()}")

        invokeChangeSignature(element, elementAtCaret, project, editor)
    }

    override fun invoke(project: Project, elements: Array<PsiElement>, dataContext: DataContext?) {
        val element = elements.singleOrNull()?.unwrapped ?: return
        if (element !is JetElement) throw AssertionError("This handler must be invoked for Kotlin elements only: ${element.getText()}")

        val editor = dataContext?.let { CommonDataKeys.EDITOR.getData(it) }
        invokeChangeSignature(element, element, project, editor)
    }

    override fun getTargetNotFoundMessage() =
            JetRefactoringBundle.message("error.wrong.caret.position.function.or.constructor.name")

    companion object {
        public fun findTargetForRefactoring(element: PsiElement): PsiElement? {
            val elementParent = element.getParent()

            if ((elementParent is JetNamedFunction || elementParent is JetClass || elementParent is JetProperty)
                && (elementParent as JetNamedDeclaration).getNameIdentifier() === element) return elementParent

            if (elementParent is JetParameter) {
                val primaryConstructor = PsiTreeUtil.getParentOfType(elementParent, javaClass<JetPrimaryConstructor>())
                if (elementParent.hasValOrVar()
                    && (elementParent.getNameIdentifier() === element || elementParent.getValOrVarKeyword() === element)
                    && primaryConstructor != null
                    && primaryConstructor.getValueParameterList() === elementParent.getParent()) return elementParent
            }

            if (elementParent is JetSecondaryConstructor && elementParent.getConstructorKeyword() === element) return elementParent

            element.getStrictParentOfType<JetParameterList>()?.let { parameterList ->
                return PsiTreeUtil.getParentOfType(parameterList, javaClass<JetFunction>(), javaClass<JetProperty>(), javaClass<JetClass>())
            }

            element.getStrictParentOfType<JetTypeParameterList>()?.let { typeParameterList ->
                return PsiTreeUtil.getParentOfType(typeParameterList, javaClass<JetFunction>(), javaClass<JetProperty>(), javaClass<JetClass>())
            }

            val call: JetCallElement? = PsiTreeUtil.getParentOfType(element,
                                                                   javaClass<JetCallExpression>(),
                                                                   javaClass<JetDelegatorToSuperCall>(),
                                                                   javaClass<JetConstructorDelegationCall>())
            val calleeExpr = call?.let {
                val callee = it.getCalleeExpression()
                (callee as? JetConstructorCalleeExpression)?.getConstructorReferenceExpression() ?: callee
            } ?: element.getStrictParentOfType<JetSimpleNameExpression>()

            if (calleeExpr is JetSimpleNameExpression || calleeExpr is JetConstructorDelegationReferenceExpression) {
                val jetElement = element.getStrictParentOfType<JetElement>() ?: return null

                val bindingContext = jetElement.analyze(BodyResolveMode.FULL)
                val descriptor = bindingContext[BindingContext.REFERENCE_TARGET, calleeExpr as JetReferenceExpression]

                if (descriptor is ClassDescriptor || descriptor is CallableDescriptor) return calleeExpr
            }

            return null
        }

        public fun invokeChangeSignature(element: JetElement, context: PsiElement, project: Project, editor: Editor?) {
            val bindingContext = element.analyze(BodyResolveMode.FULL)

            val callableDescriptor = findDescriptor(element, project, editor, bindingContext) ?: return

            if (callableDescriptor is JavaCallableMemberDescriptor) {
                val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, callableDescriptor)
                if (declaration is PsiClass) {
                    val message = RefactoringBundle.getCannotRefactorMessage(
                            RefactoringBundle.message("error.wrong.caret.position.method.or.class.name")
                    )
                    CommonRefactoringUtil.showErrorHint(project,
                                                        editor,
                                                        message,
                                                        ChangeSignatureHandler.REFACTORING_NAME, "refactoring.changeSignature")
                    return
                }
                assert(declaration is PsiMethod) { "PsiMethod expected: $callableDescriptor" }
                ChangeSignatureUtil.invokeChangeSignatureOn(declaration as PsiMethod, project)
                return
            }

            if (callableDescriptor.isDynamic()) {
                if (editor != null) {
                    CodeInsightUtils.showErrorHint(project, editor, "Change signature is not applicable to dynamically invoked functions", "Change Signature", null)
                }
                return
            }

            runChangeSignature(project, callableDescriptor, JetChangeSignatureConfiguration.Empty, bindingContext, context, null)
        }

        private fun getDescriptor(bindingContext: BindingContext, element: PsiElement): DeclarationDescriptor? {
            val descriptor = when (element) {
                is JetReferenceExpression -> bindingContext[BindingContext.REFERENCE_TARGET, element]
                else -> bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, element]
            }
            return if (descriptor is ClassDescriptor) descriptor.getUnsubstitutedPrimaryConstructor() else descriptor
        }

        public fun findDescriptor(element: PsiElement, project: Project, editor: Editor?, bindingContext: BindingContext): CallableDescriptor? {
            if (!CommonRefactoringUtil.checkReadOnlyStatus(project, element)) return null

            var descriptor = getDescriptor(bindingContext, element)

            return when (descriptor) {
                is FunctionDescriptor -> {
                    if (descriptor.getValueParameters().any { it.getVarargElementType() != null }) {
                        val message = JetRefactoringBundle.message("error.cant.refactor.vararg.functions")
                        CommonRefactoringUtil.showErrorHint(project, editor, message, ChangeSignatureHandler.REFACTORING_NAME, HelpID.CHANGE_SIGNATURE)
                        return null
                    }

                    if (descriptor.getKind() === SYNTHESIZED) {
                        val message = JetRefactoringBundle.message("cannot.refactor.synthesized.function", descriptor.getName())
                        CommonRefactoringUtil.showErrorHint(project, editor, message, ChangeSignatureHandler.REFACTORING_NAME, HelpID.CHANGE_SIGNATURE)
                        return null
                    }

                    descriptor
                }

                is PropertyDescriptor, is ValueParameterDescriptor -> descriptor as CallableDescriptor

                else -> {
                    val message = RefactoringBundle.getCannotRefactorMessage(JetRefactoringBundle.message("error.wrong.caret.position.function.or.constructor.name"))
                    CommonRefactoringUtil.showErrorHint(project, editor, message, ChangeSignatureHandler.REFACTORING_NAME, HelpID.CHANGE_SIGNATURE)
                    null
                }
            }
        }
    }
}
