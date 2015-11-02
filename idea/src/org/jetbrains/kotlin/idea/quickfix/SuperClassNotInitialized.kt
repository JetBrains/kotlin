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
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.core.refactoring.createPrimaryConstructorParameterListIfAbsent
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.IndexedParametersSubstitution
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

public object SuperClassNotInitialized : KotlinIntentionActionsFactory() {
    private val DISPLAY_MAX_PARAMS = 5

    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val delegator = diagnostic.getPsiElement() as KtDelegatorToSuperClass
        val classOrObjectDeclaration = delegator.getParent().getParent() as? KtClassOrObject ?: return emptyList()

        val typeRef = delegator.getTypeReference() ?: return emptyList()
        val type = typeRef.analyze()[BindingContext.TYPE, typeRef] ?: return emptyList()
        if (type.isError()) return emptyList()

        val superClass = (type.getConstructor().getDeclarationDescriptor() as? ClassDescriptor) ?: return emptyList()
        val classDescriptor = delegator.getResolutionFacade().resolveToDescriptor(classOrObjectDeclaration) as ClassDescriptor
        val constructors = superClass.getConstructors().filter { it.isVisible(classDescriptor) }
        if (constructors.isEmpty()) return emptyList() // no accessible constructor

        val fixes = ArrayList<IntentionAction>()

        fixes.add(AddParenthesisFix(delegator, putCaretIntoParenthesis = constructors.singleOrNull()?.getValueParameters()?.isNotEmpty() ?: true))

        if (classOrObjectDeclaration is KtClass) {
            val superType = classDescriptor.getTypeConstructor().getSupertypes().firstOrNull { it.getConstructor().getDeclarationDescriptor() == superClass }
            if (superType != null) {
                val substitutor = IndexedParametersSubstitution(superClass.typeConstructor, superType.arguments).buildSubstitutor()

                val substitutedConstructors = constructors
                        .filter { it.getValueParameters().isNotEmpty() }
                        .map { it.substitute(substitutor) as ConstructorDescriptor }

                if (substitutedConstructors.isNotEmpty()) {
                    val parameterTypes: List<List<KotlinType>> = substitutedConstructors.map {
                        it.getValueParameters().map { it.getType() }
                    }

                    fun canRenderOnlyFirstParameters(n: Int) = parameterTypes.map { it.take(n) }.toSet().size() == parameterTypes.size()

                    val maxParams = parameterTypes.map { it.size() }.max()!!
                    val maxParamsToDisplay = if (maxParams <= DISPLAY_MAX_PARAMS) {
                        maxParams
                    }
                    else {
                        (DISPLAY_MAX_PARAMS..maxParams-1).firstOrNull(::canRenderOnlyFirstParameters) ?: maxParams
                    }

                    for ((constructor, types) in substitutedConstructors.zip(parameterTypes)) {
                        val typesRendered = types.take(maxParamsToDisplay).map { DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(it) }
                        val parameterString = typesRendered.joinToString(", ", "(", if (types.size() <= maxParamsToDisplay) ")" else ",...)")
                        val text = "Add constructor parameters from " + superClass.getName().asString() + parameterString
                        fixes.addIfNotNull(AddParametersFix.create(delegator, classOrObjectDeclaration, constructor, text))
                    }
                }
            }
        }

        return fixes
    }

    private class AddParenthesisFix(
            element: KtDelegatorToSuperClass,
            val putCaretIntoParenthesis: Boolean
    ) : KotlinQuickFixAction<KtDelegatorToSuperClass>(element), HighPriorityAction {

        override fun getFamilyName() = "Change to constructor invocation" //TODO?

        override fun getText() = getFamilyName()

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val newSpecifier = element.replaced(KtPsiFactory(project).createDelegatorToSuperCall(element.getText() + "()"))

            if (putCaretIntoParenthesis) {
                if (editor != null) {
                    val offset = newSpecifier.getValueArgumentList()!!.getLeftParenthesis()!!.endOffset
                    editor.moveCaret(offset)
                    if (!ApplicationManager.getApplication().isUnitTestMode()) {
                        ShowParameterInfoHandler.invoke(project, editor, file, offset - 1, null)
                    }
                }
            }
        }
    }

    private class AddParametersFix(
            element: KtDelegatorToSuperClass,
            private val classDeclaration: KtClass,
            private val parametersToAdd: Collection<KtParameter>,
            private val argumentText: String,
            private val text: String
    ) : KotlinQuickFixAction<KtDelegatorToSuperClass>(element) {

        companion object {
            fun create(
                    element: KtDelegatorToSuperClass,
                    classDeclaration: KtClass,
                    superConstructor: ConstructorDescriptor,
                    text: String
            ): AddParametersFix? {
                val superParameters = superConstructor.getValueParameters()
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

                    val nameString = parameter.getName().asString()
                    val existingParameter = oldParameters.firstOrNull { it.getName() == nameString }
                    if (existingParameter != null) {
                        val type = (existingParameter.resolveToDescriptor() as ValueParameterDescriptor).getType()
                        if (type.isSubtypeOf(parameter.getType())) continue // use existing parameter
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

            val delegatorCall = factory.createDelegatorToSuperCall(element.text + "(" + argumentText + ")")
            element.replace(delegatorCall)

            ShortenReferences.DEFAULT.process(typeRefsToShorten)
        }
    }
}
