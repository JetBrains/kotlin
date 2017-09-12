/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.idea.refactoring.*
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiFactory.CallableBuilder.Target.READ_ONLY_PROPERTY
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.getStartOffsetIn
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.supertypes
import java.util.*

class ConvertFunctionToPropertyIntention : SelfTargetingIntention<KtNamedFunction>(KtNamedFunction::class.java, "Convert function to property"), LowPriorityAction {
    private var KtNamedFunction.typeFqNameToAdd: String? by UserDataProperty(Key.create("TYPE_FQ_NAME_TO_ADD"))

    private inner class Converter(
            project: Project,
            private val editor: Editor?,
            descriptor: FunctionDescriptor
    ) : CallableRefactoring<FunctionDescriptor>(project, descriptor, text) {
        private val elementsToShorten = ArrayList<KtElement>()

        private val newName: String by lazy {
            val name = callableDescriptor.name
            (SyntheticJavaPropertyDescriptor.propertyNameByGetMethodName(name) ?: name).asString()
        }

        private fun convertFunction(originalFunction: KtNamedFunction, psiFactory: KtPsiFactory) {
            val propertyString = KtPsiFactory.CallableBuilder(READ_ONLY_PROPERTY).apply {
                // make sure to capture all comments and line breaks
                modifier(originalFunction.text.substring(0, originalFunction.funKeyword!!.getStartOffsetIn(originalFunction)))
                typeParams(originalFunction.typeParameters.map { it.text })
                originalFunction.receiverTypeReference?.let { receiver(it.text) }
                name(newName)
                originalFunction.getReturnTypeReference()?.let { returnType(it.text) }
                typeConstraints(originalFunction.typeConstraints.map { it.text })

                if (originalFunction.equalsToken != null) {
                    getterExpression(originalFunction.bodyExpression!!.text, breakLine = originalFunction.typeReference != null)
                }
                else {
                    (originalFunction.bodyExpression as? KtBlockExpression)?.let { body ->
                        transform {
                            append("\nget() ")
                            append(body.text)
                        }
                    }
                }
            }.asString()

            val replaced = originalFunction.replaced(psiFactory.createDeclaration<KtProperty>(propertyString))

            editor?.caretModel?.moveToOffset(replaced.nameIdentifier!!.endOffset)
        }

        override fun performRefactoring(descriptorsForChange: Collection<CallableDescriptor>) {
            val conflicts = MultiMap<PsiElement, String>()
            val getterName = JvmAbi.getterName(callableDescriptor.name.asString())
            val callables = getAffectedCallables(project, descriptorsForChange)
            val kotlinCalls = ArrayList<KtCallElement>()
            val kotlinRefsToRename = ArrayList<PsiReference>()
            val foreignRefs = ArrayList<PsiReference>()
            for (callable in callables) {
                if (callable !is PsiNamedElement) continue

                if (!checkModifiable(callable)) {
                    reportDeclarationConflict(conflicts, callable) { "Can't modify $it" }
                }

                if (callable is KtNamedFunction) {
                    if (callable.typeReference == null) {
                        val functionDescriptor = callable.unsafeResolveToDescriptor(BodyResolveMode.PARTIAL) as FunctionDescriptor
                        val type = functionDescriptor.returnType
                        val typeToInsert = when {
                                               type == null || type.isError -> null
                                               type.constructor.isDenotable -> type
                                               else -> type.supertypes().firstOrNull { it.constructor.isDenotable }
                                           } ?: functionDescriptor.builtIns.nullableAnyType
                        callable.typeFqNameToAdd = IdeDescriptorRenderers.SOURCE_CODE.renderType(typeToInsert)
                    }

                    callableDescriptor.getContainingScope()
                            ?.findVariable(callableDescriptor.name, NoLookupLocation.FROM_IDE)
                            ?.let { DescriptorToSourceUtilsIde.getAnyDeclaration(project, it) }
                            ?.let { reportDeclarationConflict(conflicts, it) { "$it already exists" } }
                }

                if (callable is PsiMethod) {
                    callable.containingClass
                            ?.findMethodsByName(getterName, true)
                            // as is necessary here: see KT-10386
                            ?.firstOrNull { it.parameterList.parametersCount == 0 && !callables.contains(it.namedUnwrappedElement as PsiElement?) }
                            ?.let { reportDeclarationConflict(conflicts, it) { "$it already exists" } }
                }

                val usages = ReferencesSearch.search(callable)
                for (usage in usages) {
                    if (usage is KtSimpleNameReference) {
                        val expression = usage.expression
                        val callElement = expression.getParentOfTypeAndBranch<KtCallElement> { calleeExpression }
                        if (callElement != null && expression.getStrictParentOfType<KtCallableReferenceExpression>() == null) {
                            if (callElement.typeArguments.isNotEmpty()) {
                                conflicts.putValue(
                                        callElement,
                                        "Type arguments will be lost after conversion: ${StringUtil.htmlEmphasize(callElement.text)}"
                                )
                            }

                            if (callElement.valueArguments.isNotEmpty()) {
                                conflicts.putValue(
                                        callElement,
                                        "Call with arguments will be skipped: ${StringUtil.htmlEmphasize(callElement.text)}"
                                )
                                continue
                            }

                            kotlinCalls.add(callElement)
                        }
                        else {
                            kotlinRefsToRename.add(usage)
                        }
                    }
                    else {
                        foreignRefs.add(usage)
                    }
                }
            }

            project.checkConflictsInteractively(conflicts) {
                project.executeWriteCommand(text) {
                    val psiFactory = KtPsiFactory(project)
                    val newGetterName = JvmAbi.getterName(newName)
                    val newRefExpr = psiFactory.createExpression(newName)

                    kotlinCalls.forEach { it.replace(newRefExpr) }
                    kotlinRefsToRename.forEach { it.handleElementRename(newName) }
                    foreignRefs.forEach { it.handleElementRename(newGetterName) }
                    callables.forEach {
                        when (it) {
                            is KtNamedFunction -> convertFunction(it, psiFactory)
                            is PsiMethod -> it.name = newGetterName
                        }
                    }

                    ShortenReferences.DEFAULT.process(elementsToShorten)
                }
            }
        }
    }

    override fun startInWriteAction(): Boolean = false

    override fun isApplicableTo(element: KtNamedFunction, caretOffset: Int): Boolean {
        val identifier = element.nameIdentifier ?: return false
        if (!identifier.textRange.containsOffset(caretOffset)) return false

        if (element.valueParameters.isNotEmpty() || element.isLocal) return false

        val name = element.name!!
        if (name == "invoke" || name == "iterator" || Name.identifier(name) in OperatorConventions.UNARY_OPERATION_NAMES.inverse().keys) {
            return false
        }

        val descriptor = element.analyze(BodyResolveMode.PARTIAL)[BindingContext.DECLARATION_TO_DESCRIPTOR, element] as? FunctionDescriptor
                         ?: return false
        val returnType = descriptor.returnType ?: return false
        return !KotlinBuiltIns.isUnit(returnType) && !KotlinBuiltIns.isNothing(returnType)
    }

    override fun applyTo(element: KtNamedFunction, editor: Editor?) {
        val context = element.analyze(BodyResolveMode.PARTIAL)
        val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, element] as FunctionDescriptor
        Converter(element.project, editor, descriptor).run()
    }
}
