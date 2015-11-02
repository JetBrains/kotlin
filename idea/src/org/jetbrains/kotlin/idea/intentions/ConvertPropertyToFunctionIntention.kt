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

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.refactoring.checkConflictsInteractively
import org.jetbrains.kotlin.idea.core.refactoring.reportDeclarationConflict
import org.jetbrains.kotlin.idea.refactoring.CallableRefactoring
import org.jetbrains.kotlin.idea.refactoring.getAffectedCallables
import org.jetbrains.kotlin.idea.refactoring.getContainingScope
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import java.util.*

public class ConvertPropertyToFunctionIntention : SelfTargetingIntention<KtProperty>(javaClass(), "Convert property to function"), LowPriorityAction {
    private inner class Converter(
            project: Project,
            descriptor: CallableDescriptor
    ): CallableRefactoring<CallableDescriptor>(project, descriptor, getText()) {
        private val newName: String = JvmAbi.getterName(callableDescriptor.name.asString())

        private fun convertProperty(originalProperty: KtProperty, psiFactory: KtPsiFactory) {
            val property = originalProperty.copy() as KtProperty;
            val getter = property.getGetter();

            val sampleFunction = psiFactory.createFunction("fun foo() {\n\n}");

            property.getValOrVarKeyword().replace(sampleFunction.getFunKeyword()!!);
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
            property.setName(newName)

            originalProperty.replace(psiFactory.createFunction(property.getText()))
        }

        override fun performRefactoring(descriptorsForChange: Collection<CallableDescriptor>) {
            val propertyName = callableDescriptor.getName().asString()
            val nameChanged = propertyName != newName
            val getterName = JvmAbi.getterName(callableDescriptor.getName().asString())
            val conflicts = MultiMap<PsiElement, String>()
            val callables = getAffectedCallables(project, descriptorsForChange)
            val kotlinRefsToReplaceWithCall = ArrayList<KtSimpleNameExpression>()
            val refsToRename = ArrayList<PsiReference>()
            val javaRefsToReplaceWithCall = ArrayList<PsiReferenceExpression>()
            for (callable in callables) {
                if (callable !is PsiNamedElement) continue

                if (!checkModifiable(callable)) {
                    val renderedCallable = RefactoringUIUtil.getDescription(callable, true).capitalize()
                    conflicts.putValue(callable, "Can't modify $renderedCallable")
                }

                if (callable is KtProperty) {
                    callableDescriptor.getContainingScope()
                            ?.findFunction(callableDescriptor.name, NoLookupLocation.FROM_IDE) { it.valueParameters.isEmpty() }
                            ?.let { DescriptorToSourceUtilsIde.getAnyDeclaration(project, it) }
                            ?.let { reportDeclarationConflict(conflicts, it) { "$it already exists" } }
                }
                else if (callable is PsiMethod) {
                    callable.getContainingClass()
                            ?.findMethodsByName(propertyName, true)
                            ?.firstOrNull { it.getParameterList().getParametersCount() == 0 && it.namedUnwrappedElement !in callables }
                            ?.let { reportDeclarationConflict(conflicts, it) { "$it already exists" } }
                }

                val usages = ReferencesSearch.search(callable)
                for (usage in usages) {
                    if (usage is KtReference) {
                        if (usage is KtSimpleNameReference) {
                            val expression = usage.expression
                            if (expression.getCall(expression.analyze(BodyResolveMode.PARTIAL)) != null
                                && expression.getStrictParentOfType<KtCallableReferenceExpression>() == null) {
                                kotlinRefsToReplaceWithCall.add(expression)
                            }
                            else if (nameChanged) {
                                refsToRename.add(usage)
                            }
                        }
                        else {
                            val refElement = usage.getElement()
                            conflicts.putValue(
                                    refElement,
                                    "Unrecognized reference will be skipped: " + StringUtil.htmlEmphasize(refElement.getText())
                            )
                        }
                        continue
                    }

                    val refElement = usage.getElement()

                    if (refElement.getText().endsWith(getterName)) continue

                    if (usage is PsiJavaReference) {
                        if (usage.resolve() is PsiField && usage is PsiReferenceExpression) {
                            javaRefsToReplaceWithCall.add(usage)
                        }
                        continue
                    }

                    conflicts.putValue(
                            refElement,
                            "Can't replace foreign reference with call expression: " + StringUtil.htmlEmphasize(refElement.getText())
                    )
                }
            }

            project.checkConflictsInteractively(conflicts) {
                project.executeWriteCommand(getText()) {
                    val kotlinPsiFactory = KtPsiFactory(project)
                    val javaPsiFactory = PsiElementFactory.SERVICE.getInstance(project)
                    val newKotlinCallExpr = kotlinPsiFactory.createExpression("$newName()")

                    kotlinRefsToReplaceWithCall.forEach { it.replace(newKotlinCallExpr) }
                    refsToRename.forEach { it.handleElementRename(newName) }
                    javaRefsToReplaceWithCall.forEach {
                        val getterRef = it.handleElementRename(newName)
                        getterRef.replace(javaPsiFactory.createExpressionFromText("${getterRef.text}()", null))
                    }
                    callables.forEach {
                        when (it) {
                            is KtProperty -> convertProperty(it, kotlinPsiFactory)
                            is PsiMethod -> it.setName(newName)
                        }
                    }
                }
            }
        }
    }

    override fun startInWriteAction(): Boolean = false

    override fun isApplicableTo(element: KtProperty, caretOffset: Int): Boolean {
        val identifier = element.getNameIdentifier() ?: return false
        if (!identifier.getTextRange().containsOffset(caretOffset)) return false
        return element.getDelegate() == null && !element.isVar() && !element.isLocal()
    }

    override fun applyTo(element: KtProperty, editor: Editor) {
        val context = element.analyze()
        val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, element] as? CallableDescriptor ?: return
        Converter(element.getProject(), descriptor).run()
    }
}
