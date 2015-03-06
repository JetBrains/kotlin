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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.refactoring.CallableRefactoring
import org.jetbrains.kotlin.psi.JetPsiFactory
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.psiUtil.siblings
import com.intellij.util.containers.MultiMap
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.refactoring.getAffectedCallables
import java.util.ArrayList
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.util.RefactoringUIUtil
import org.jetbrains.kotlin.idea.search.usagesSearch.DefaultSearchHelper
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchTarget
import org.jetbrains.kotlin.idea.search.usagesSearch.search
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.idea.references.JetReference
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiField
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiElementFactory
import org.jetbrains.kotlin.idea.refactoring.checkConflictsInteractively
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.JetCallableReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import com.intellij.psi.PsiJavaReference
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.JetExpressionImpl
import org.jetbrains.kotlin.codegen.PropertyCodegen
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.refactoring.reportDeclarationConflict
import org.jetbrains.kotlin.idea.refactoring.getContainingScope
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde

public class ConvertPropertyToFunctionIntention : JetSelfTargetingIntention<JetProperty>(
        "convert.property.to.function.intention", javaClass()
) {
    private inner class Convertor(
            project: Project,
            descriptor: CallableDescriptor,
            context: BindingContext): CallableRefactoring<CallableDescriptor>(project, descriptor, context, getText()) {
        private fun convertJetProperty(originalProperty: JetProperty, psiFactory: JetPsiFactory) {
            val property = originalProperty.copy() as JetProperty;
            val getter = property.getGetter();

            val sampleFunction = psiFactory.createFunction("fun foo() {\n\n}");

            property.getValOrVarNode().getPsi().replace(sampleFunction.getFunToken());
            property.addAfter(psiFactory.createParameterList("()"), property.getNameIdentifier());
            if (property.getInitializer() == null) {
                if (getter != null) {
                    val dropGetterTo = (getter.getEqualsToken() ?: getter.getBodyExpression())
                            ?.siblings(forward = false, withItself = false)
                            ?.firstOrNull { it !is PsiWhiteSpace }
                    getter.deleteChildRange(getter.getFirstChild(), dropGetterTo)

                    val dropPropertyFrom = getter
                            .siblings(forward = false, withItself = false)
                            .first { it !is PsiWhiteSpace }
                            .getNextSibling()
                    property.deleteChildRange(dropPropertyFrom, getter.getPrevSibling())
                }
            }
            originalProperty.replace(psiFactory.createFunction(property.getText()))
        }

        override fun performRefactoring(descriptorsForChange: Collection<CallableDescriptor>) {
            val propertyName = callableDescriptor.getName().asString()
            val getterName = PropertyCodegen.getterName(callableDescriptor.getName())
            val conflicts = MultiMap<PsiElement, String>()
            val callables = getAffectedCallables(project, descriptorsForChange)
            val kotlinRefs = ArrayList<JetSimpleNameExpression>()
            val foreignRefsToRename = ArrayList<PsiReference>()
            val javaRefsToReplaceWithCall = ArrayList<PsiReferenceExpression>()
            for (callable in callables) {
                if (callable !is PsiNamedElement) continue

                if (!checkModifiable(callable)) {
                    val renderedCallable = RefactoringUIUtil.getDescription(callable, true).capitalize()
                    conflicts.putValue(callable, "Can't modify $renderedCallable")
                }

                if (callable is JetProperty) {
                    callableDescriptor.getContainingScope(bindingContext)
                            ?.getFunctions(callableDescriptor.getName())
                            ?.firstOrNull { it.getValueParameters().isEmpty() }
                            ?.let { DescriptorToSourceUtilsIde.getAnyDeclaration(project, it) }
                            ?.let { reportDeclarationConflict(conflicts, it) { "$it already exists" } }
                }
                else if (callable is PsiMethod) {
                    callable.getContainingClass()
                            ?.findMethodsByName(propertyName, true)
                            ?.firstOrNull { it.getParameterList().getParametersCount() == 0 && it.namedUnwrappedElement !in callables }
                            ?.let { reportDeclarationConflict(conflicts, it) { "$it already exists" } }
                }

                val usages = DefaultSearchHelper<PsiNamedElement>().newRequest(UsagesSearchTarget(callable)).search()
                for (usage in usages) {
                    if (usage is JetReference) {
                        if (usage is JetSimpleNameReference) {
                            val expression = usage.expression
                            if (expression.getCall(expression.analyze()) != null
                                && expression.getStrictParentOfType<JetCallableReferenceExpression>() == null) {
                                kotlinRefs.add(expression)
                            }
                        }
                        else {
                            val refElement = usage.getElement()
                            conflicts.putValue(
                                    refElement,
                                    "Unrecognized reference will be skipped: ${StringUtil.htmlEmphasize(refElement.getText())}"
                            )
                        }
                        continue
                    }

                    val refElement = usage.getElement()

                    if (refElement.getText().endsWith(getterName)) {
                        foreignRefsToRename.add(usage)
                        continue
                    }

                    if (usage is PsiJavaReference) {
                        if (usage.resolve() is PsiField && usage is PsiReferenceExpression) {
                            javaRefsToReplaceWithCall.add(usage)
                        }
                        continue
                    }

                    conflicts.putValue(
                            refElement,
                            "Can't replace foreign reference with call expression: ${StringUtil.htmlEmphasize(refElement.getText())}"
                    )
                }
            }

            project.checkConflictsInteractively(conflicts) {
                project.executeWriteCommand(getText()) {
                    val kotlinPsiFactory = JetPsiFactory(project)
                    val javaPsiFactory = PsiElementFactory.SERVICE.getInstance(project)

                    callables.forEach {
                        when (it) {
                            is JetProperty -> convertJetProperty(it, kotlinPsiFactory)
                            is PsiMethod -> it.setName(propertyName)
                        }
                    }
                    kotlinRefs.forEach { it.replace(kotlinPsiFactory.createExpression("${it.getText()}()")) }
                    foreignRefsToRename.forEach { it.handleElementRename(propertyName) }
                    javaRefsToReplaceWithCall.forEach { it.replace(javaPsiFactory.createExpressionFromText("${it.getText()}()", null)) }
                }
            }
        }
    }

    override fun startInWriteAction(): Boolean = false

    override fun isApplicableTo(element: JetProperty): Boolean {
        throw IllegalStateException("isApplicableTo(JetExpressionImpl, Editor) should be called instead")
    }

    override fun isApplicableTo(element: JetProperty, editor: Editor): Boolean {
        val elementAtCaret = element.getContainingFile().findElementAt(editor.getCaretModel().getOffset())
        if (!(element.getNameIdentifier()?.isAncestor(elementAtCaret) ?: false)) return false

        return element.getDelegate() == null && !element.isVar() && !element.isLocal()
    }

    override fun applyTo(element: JetProperty, editor: Editor) {
        val context = element.analyze()
        val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, element] as? CallableDescriptor ?: return
        Convertor(element.getProject(), descriptor, context).run()
    }
}
