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
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.refactoring.checkConflictsInteractively
import org.jetbrains.kotlin.idea.core.refactoring.reportDeclarationConflict
import org.jetbrains.kotlin.idea.refactoring.CallableRefactoring
import org.jetbrains.kotlin.idea.refactoring.getAffectedCallables
import org.jetbrains.kotlin.idea.refactoring.getContainingScope
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import org.jetbrains.kotlin.idea.search.usagesSearch.DefaultSearchHelper
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchTarget
import org.jetbrains.kotlin.idea.search.usagesSearch.search
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.supertypes
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import java.util.ArrayList

public class ConvertFunctionToPropertyIntention : JetSelfTargetingIntention<JetNamedFunction>(javaClass(), "Convert function to property"), LowPriorityAction {
    private var JetNamedFunction.typeFqNameToAdd: String? by UserDataProperty(Key.create("TYPE_FQ_NAME_TO_ADD"))

    private inner class Converter(
            project: Project,
            descriptor: FunctionDescriptor,
            context: BindingContext
    ): CallableRefactoring<FunctionDescriptor>(project, descriptor, context, getText()) {
        private val elementsToShorten = ArrayList<JetElement>()

        private fun convertFunction(originalFunction: JetNamedFunction, psiFactory: JetPsiFactory) {
            val function = originalFunction.copy() as JetNamedFunction

            val propertySample = psiFactory.createProperty("val foo: Int get() = 1")

            val needsExplicitType = function.getTypeReference() == null
            if (needsExplicitType) {
                originalFunction.typeFqNameToAdd?.let { function.setTypeReference(psiFactory.createType(it)) }
            }

            function.getFunKeyword()!!.replace(propertySample.getValOrVarKeyword())
            function.getValueParameterList()?.delete()
            val insertAfter = (function.getEqualsToken() ?: function.getBodyExpression())
                    ?.siblings(forward = false, withItself = false)
                    ?.firstOrNull { it !is PsiWhiteSpace }
            if (insertAfter != null) {
                function.addAfter(psiFactory.createParameterList("()"), insertAfter)
                function.addAfter(propertySample.getGetter()!!.getNamePlaceholder(), insertAfter)
                function.addAfter(psiFactory.createNewLine(), insertAfter)
            }

            val property = originalFunction.replace(psiFactory.createProperty(function.getText())) as JetProperty
            if (needsExplicitType) {
                elementsToShorten.add(property.getTypeReference()!!)
            }
        }

        override fun performRefactoring(descriptorsForChange: Collection<CallableDescriptor>) {
            val conflicts = MultiMap<PsiElement, String>()
            val getterName = JvmAbi.getterName(callableDescriptor.getName().asString())
            val callables = getAffectedCallables(project, descriptorsForChange)
            val kotlinCalls = ArrayList<JetCallElement>()
            val foreignRefs = ArrayList<PsiReference>()
            for (callable in callables) {
                if (callable !is PsiNamedElement) continue

                if (!checkModifiable(callable)) {
                    reportDeclarationConflict(conflicts, callable) { "Can't modify $it" }
                }

                if (callable is JetNamedFunction) {
                    if (callable.getTypeReference() == null) {
                        val type = (callable.resolveToDescriptor() as FunctionDescriptor).getReturnType()
                        val typeToInsert = when {
                                               type == null || type.isError() -> null
                                               type.getConstructor().isDenotable() -> type
                                               else -> type.supertypes().firstOrNull { it.getConstructor().isDenotable() }
                                           } ?: KotlinBuiltIns.getInstance().getNullableAnyType()
                        callable.typeFqNameToAdd = IdeDescriptorRenderers.SOURCE_CODE.renderType(typeToInsert)
                    }

                    callableDescriptor.getContainingScope(bindingContext)
                            ?.getProperties(callableDescriptor.getName())
                            ?.firstOrNull()
                            ?.let { DescriptorToSourceUtilsIde.getAnyDeclaration(project, it) }
                            ?.let { reportDeclarationConflict(conflicts, it) { "$it already exists" } }
                }

                if (callable is PsiMethod) {
                    callable.getContainingClass()
                            ?.findMethodsByName(getterName, true)
                            ?.firstOrNull { it.getParameterList().getParametersCount() == 0 && it.namedUnwrappedElement !in callables }
                            ?.let { reportDeclarationConflict(conflicts, it) { "$it already exists" } }
                }

                val usages = DefaultSearchHelper<PsiNamedElement>().newRequest(UsagesSearchTarget(callable)).search()
                for (usage in usages) {
                    if (usage is JetSimpleNameReference) {
                        val callElement = usage.expression.getParentOfTypeAndBranch<JetCallElement> { getCalleeExpression() }
                        if (callElement != null) {
                            if (callElement.getTypeArguments().isNotEmpty()) {
                                conflicts.putValue(
                                        callElement,
                                        "Type arguments will be lost after conversion: ${StringUtil.htmlEmphasize(callElement.getText())}"
                                )
                            }

                            if (callElement.getValueArguments().isNotEmpty()) {
                                conflicts.putValue(
                                        callElement,
                                        "Call with arguments will be skipped: ${StringUtil.htmlEmphasize(callElement.getText())}"
                                )
                                continue
                            }

                            kotlinCalls.add(callElement)
                            continue
                        }
                    }
                    else {
                        foreignRefs.add(usage)
                    }
                }
            }

            project.checkConflictsInteractively(conflicts) {
                project.executeWriteCommand(getText()) {
                    val psiFactory = JetPsiFactory(project)

                    kotlinCalls.forEach { it.replace(it.getCalleeExpression()!!) }
                    foreignRefs.forEach { it.handleElementRename(getterName) }
                    callables.forEach {
                        when (it) {
                            is JetNamedFunction -> convertFunction(it, psiFactory)
                            is PsiMethod -> it.setName(getterName)
                        }
                    }

                    ShortenReferences.DEFAULT.process(elementsToShorten)
                }
            }
        }
    }

    override fun startInWriteAction(): Boolean = false

    override fun isApplicableTo(element: JetNamedFunction, caretOffset: Int): Boolean {
        val identifier = element.getNameIdentifier() ?: return false
        if (!identifier.getTextRange().containsOffset(caretOffset)) return false

        if (element.getValueParameters().isNotEmpty() || element.isLocal()) return false

        val name = element.getName()!!
        if (name == "invoke" || name == "iterator" || Name.identifier(name) in OperatorConventions.UNARY_OPERATION_NAMES.inverse().keySet()) {
            return false
        }

        val descriptor = element.analyze()[BindingContext.DECLARATION_TO_DESCRIPTOR, element] as? FunctionDescriptor ?: return false
        val returnType = descriptor.getReturnType() ?: return false
        return !KotlinBuiltIns.isUnit(returnType) && !KotlinBuiltIns.isNothing(returnType)
    }

    override fun applyTo(element: JetNamedFunction, editor: Editor) {
        val context = element.analyze()
        val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, element] as FunctionDescriptor
        Converter(element.getProject(), descriptor, context).run()
    }
}
