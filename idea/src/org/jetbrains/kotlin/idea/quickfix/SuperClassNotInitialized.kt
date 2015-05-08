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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.replaced
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.TypeUtils
import java.util.ArrayList

public object SuperClassNotInitialized : JetIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction>? {
        val delegator = diagnostic.getPsiElement() as JetDelegatorToSuperClass
        val classOrObjectDeclaration = delegator.getParent().getParent() as? JetClassOrObject ?: return null

        val typeRef = delegator.getTypeReference() ?: return null
        val type = typeRef.analyze()[BindingContext.TYPE, typeRef] ?: return null
        if (type.isError()) return null

        val superClass = (type.getConstructor().getDeclarationDescriptor() as? ClassDescriptor) ?: return null
        val classDescriptor = delegator.getResolutionFacade().resolveToDescriptor(classOrObjectDeclaration) as ClassDescriptor
        val constructors = superClass.getConstructors().filter { it.isVisible(classDescriptor) }
        if (constructors.isEmpty()) return null // no accessible constructor

        val fixes = ArrayList<IntentionAction>()

        fixes.add(AddParenthesisFix(delegator, putCaretIntoParenthesis = constructors.singleOrNull()?.getValueParameters()?.isNotEmpty() ?: true))

        if (classOrObjectDeclaration is JetClass) {
            val superType = classDescriptor.getTypeConstructor().getSupertypes().first { it.getConstructor().getDeclarationDescriptor() == superClass }
            val typeArgsMap = superClass.getTypeConstructor().getParameters().zip(superType.getArguments()).toMap()
            val substitutor = TypeUtils.makeSubstitutorForTypeParametersMap(typeArgsMap)

            for (constructor in constructors) {
                if (constructor.getValueParameters().isNotEmpty() && constructor.getValueParameters().none { it.getType().isError() }) {
                    val substitutedConstructor = constructor.substitute(substitutor) as ConstructorDescriptor
                    fixes.add(AddParametersFix(delegator, classOrObjectDeclaration, substitutedConstructor))
                }
            }
        }

        return fixes
    }

    private class AddParenthesisFix(
            element: JetDelegatorToSuperClass,
            val putCaretIntoParenthesis: Boolean
    ) : JetIntentionAction<JetDelegatorToSuperClass>(element) {

        override fun getFamilyName() = "Change to constructor invocation" //TODO?

        override fun getText() = getFamilyName()

        override fun invoke(project: Project, editor: Editor?, file: JetFile) {
            val newSpecifier = element.replaced(JetPsiFactory(project).createDelegatorToSuperCall(element.getText() + "()"))

            if (putCaretIntoParenthesis) {
                editor?.moveCaret(newSpecifier.getValueArgumentList()!!.getLeftParenthesis()!!.getTextRange().getEndOffset())
            }
        }
    }

    private class AddParametersFix(
            element: JetDelegatorToSuperClass,
            val classDeclaration: JetClass,
            val superConstructor: ConstructorDescriptor
    ) : JetIntentionAction<JetDelegatorToSuperClass>(element) {

        override fun getFamilyName() = "Add constructor parameters from superclass"

        override fun getText(): String {
            return "Add constructor parameters from " +
                   superConstructor.getContainingDeclaration().getName().asString() +
                   DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderFunctionParameters(superConstructor)
        }

        override fun invoke(project: Project, editor: Editor?, file: JetFile) {
            val factory = JetPsiFactory(project)
            val renderer = IdeDescriptorRenderers.SOURCE_CODE

            val superParameters = superConstructor.getValueParameters()
            val parameterNames = ArrayList<String>()
            val typeRefsToShorten = ArrayList<JetTypeReference>()
            if (!superParameters.isEmpty()) {
                val parameterList = classDeclaration.getOrCreatePrimaryConstructorParameterList()
                for (parameter in superParameters) {
                    val name = renderer.renderName(parameter.getName())
                    val parameterText = name + ":" + renderer.renderType(parameter.getType())
                    val newParameter = parameterList.addParameter(factory.createParameter(parameterText))
                    typeRefsToShorten.add(newParameter.getTypeReference())
                    parameterNames.add(name)
                }
            }

            val delegatorCall = factory.createDelegatorToSuperCall(element.getText() + "(" + parameterNames.joinToString(",") + ")")
            element.replace(delegatorCall)

            ShortenReferences.DEFAULT.process(typeRefsToShorten)
        }
    }
}
