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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.hint.ShowParameterInfoHandler
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

object SuperClassNotInitialized : KotlinIntentionActionsFactory() {
    private val DISPLAY_MAX_PARAMS = 5

    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val delegator = diagnostic.psiElement as KtSuperTypeEntry
        val classOrObjectDeclaration = delegator.parent.parent as? KtClassOrObject ?: return emptyList()

        val typeRef = delegator.typeReference ?: return emptyList()
        val type = typeRef.analyze()[BindingContext.TYPE, typeRef] ?: return emptyList()
        if (type.isError) return emptyList()

        val superClass = (type.constructor.declarationDescriptor as? ClassDescriptor) ?: return emptyList()
        val classDescriptor = delegator.getResolutionFacade().resolveToDescriptor(classOrObjectDeclaration) as ClassDescriptor
        val constructors = superClass.constructors.filter { it.isVisible(classDescriptor) }
        if (constructors.isEmpty()) return emptyList() // no accessible constructor

        val fixes = ArrayList<IntentionAction>()

        fixes.add(AddParenthesisFix(delegator, putCaretIntoParenthesis = constructors.singleOrNull()?.valueParameters?.isNotEmpty() ?: true))

        if (classOrObjectDeclaration is KtClass) {
            val superType = classDescriptor.typeConstructor.supertypes.firstOrNull { it.constructor.declarationDescriptor == superClass }
            if (superType != null) {
                val substitutor = TypeConstructorSubstitution.create(superClass.typeConstructor, superType.arguments).buildSubstitutor()

                val substitutedConstructors = constructors
                        .filter { it.valueParameters.isNotEmpty() }
                        .mapNotNull { it.substitute(substitutor) }

                if (substitutedConstructors.isNotEmpty()) {
                    val parameterTypes: List<List<KotlinType>> = substitutedConstructors.map {
                        it.valueParameters.map { it.type }
                    }

                    fun canRenderOnlyFirstParameters(n: Int) = parameterTypes.map { it.take(n) }.toSet().size == parameterTypes.size

                    val maxParams = parameterTypes.map { it.size }.max()!!
                    val maxParamsToDisplay = if (maxParams <= DISPLAY_MAX_PARAMS) {
                        maxParams
                    }
                    else {
                        (DISPLAY_MAX_PARAMS..maxParams-1).firstOrNull(::canRenderOnlyFirstParameters) ?: maxParams
                    }

                    for ((constructor, types) in substitutedConstructors.zip(parameterTypes)) {
                        val typesRendered = types.take(maxParamsToDisplay).map { DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(it) }
                        val parameterString = typesRendered.joinToString(", ", "(", if (types.size <= maxParamsToDisplay) ")" else ",...)")
                        val text = "Add constructor parameters from " + superClass.name.asString() + parameterString
                        fixes.addIfNotNull(AddParametersFix.create(delegator, classOrObjectDeclaration, constructor, text))
                    }
                }
            }
        }

        return fixes
    }

    private class AddParenthesisFix(
            element: KtSuperTypeEntry,
            val putCaretIntoParenthesis: Boolean
    ) : KotlinQuickFixAction<KtSuperTypeEntry>(element), HighPriorityAction {

        override fun getFamilyName() = "Change to constructor invocation" //TODO?

        override fun getText() = familyName

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val newSpecifier = element.replaced(KtPsiFactory(project).createSuperTypeCallEntry(element.text + "()"))

            if (putCaretIntoParenthesis) {
                if (editor != null) {
                    val offset = newSpecifier.valueArgumentList!!.leftParenthesis!!.endOffset
                    editor.moveCaret(offset)
                    if (!ApplicationManager.getApplication().isUnitTestMode) {
                        ShowParameterInfoHandler.invoke(project, editor, file, offset - 1, null)
                    }
                }
            }
        }
    }

    private class AddParametersFix(
            element: KtSuperTypeEntry,
            private val classDeclaration: KtClass,
            private val parametersToAdd: Collection<KtParameter>,
            private val argumentText: String,
            private val text: String
    ) : KotlinQuickFixAction<KtSuperTypeEntry>(element) {

        companion object {
            fun create(
                    element: KtSuperTypeEntry,
                    classDeclaration: KtClass,
                    superConstructor: ConstructorDescriptor,
                    text: String
            ): AddParametersFix? {
                val superParameters = superConstructor.valueParameters
                assert(superParameters.isNotEmpty())

                if (superParameters.any { it.type.isError }) return null

                val argumentText = StringBuilder()
                val oldParameters = classDeclaration.getPrimaryConstructorParameters()
                val parametersToAdd = ArrayList<KtParameter>()
                for (parameter in superParameters) {
                    val nameRendered = parameter.name.render()
                    val varargElementType = parameter.varargElementType

                    if (argumentText.length > 0) {
                        argumentText.append(", ")
                    }
                    argumentText.append(if (varargElementType != null) "*$nameRendered" else nameRendered)

                    val nameString = parameter.name.asString()
                    val existingParameter = oldParameters.firstOrNull { it.name == nameString }
                    if (existingParameter != null) {
                        val type = (existingParameter.resolveToDescriptor() as VariableDescriptor).type
                        if (type.isSubtypeOf(parameter.type)) continue // use existing parameter
                    }

                    val parameterText = if (varargElementType != null)
                        "vararg " + nameRendered + ":" + IdeDescriptorRenderers.SOURCE_CODE.renderType(varargElementType)
                    else
                        nameRendered + ":" + IdeDescriptorRenderers.SOURCE_CODE.renderType(parameter.type)
                    parametersToAdd.add(KtPsiFactory(element).createParameter(parameterText))
                }

                return AddParametersFix(element, classDeclaration, parametersToAdd, argumentText.toString(), text)
            }
        }

        override fun getFamilyName() = "Add constructor parameters from superclass"

        override fun getText() = text

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val factory = KtPsiFactory(project)

            val typeRefsToShorten = ArrayList<KtTypeReference>()
            val parameterList = classDeclaration.createPrimaryConstructorParameterListIfAbsent()

            for (parameter in parametersToAdd) {
                val newParameter = parameterList.addParameter(parameter)
                typeRefsToShorten.add(newParameter.typeReference!!)
            }

            val delegatorCall = factory.createSuperTypeCallEntry(element.text + "(" + argumentText + ")")
            element.replace(delegatorCall)

            ShortenReferences.DEFAULT.process(typeRefsToShorten)
        }
    }
}
