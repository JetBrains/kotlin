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
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeUtils
import java.util.ArrayList

public class AddConstructorParametersFromSuperFix private(
        element: JetDelegatorToSuperClass,
        val classDeclaration: JetClass,
        val superConstructor: ConstructorDescriptor
) : JetIntentionAction<JetDelegatorToSuperClass>(element) {

    override fun getFamilyName() = "Add constructor parameters from superclass"

    override fun getText() = "Add constructor parameters and use them"

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        val factory = JetPsiFactory(project)
        val renderer = IdeDescriptorRenderers.SOURCE_CODE

        val superParameters = superConstructor.getValueParameters()
        val parameterNames = ArrayList<String>()
        val typeRefsToShorten = ArrayList<JetTypeReference>()
        if (!superParameters.isEmpty()) {
            val parameterList = classDeclaration.getOrCreatePrimaryConstructorParameterList()
            for (parameter in superParameters) {
                //TODO: what if type is error?
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

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): JetIntentionAction<JetDelegatorToSuperClass>? {
            val delegator = diagnostic.getPsiElement() as JetDelegatorToSuperClass
            val classDeclaration = delegator.getParent().getParent() as? JetClass ?: return null

            val typeRef = delegator.getTypeReference() ?: return null
            val type = typeRef.analyze()[BindingContext.TYPE, typeRef] ?: return null
            if (type.isError()) return null

            val superClass = (type.getConstructor().getDeclarationDescriptor() as? ClassDescriptor) ?: return null
            val classDescriptor = delegator.getResolutionFacade().resolveToDescriptor(classDeclaration) as ClassDescriptor
            val constructors = superClass.getConstructors().filter { it.isVisible(classDescriptor) }
            val constructorToUse = constructors.singleOrNull()
                                     ?: constructors.singleOrNull { it.isPrimary() } //TODO: should we select it automatically in this case?
                                     ?: return null
            //TODO: choose among multiple
            if (constructorToUse.getValueParameters().isEmpty()) return null

            val superType = classDescriptor.getTypeConstructor().getSupertypes().first { it.getConstructor().getDeclarationDescriptor() == superClass }
            val typeArgsMap = superClass.getTypeConstructor().getParameters().zip(superType.getArguments()).toMap()
            val substitutor = TypeUtils.makeSubstitutorForTypeParametersMap(typeArgsMap)
            val substitutedConstructor = constructorToUse.substitute(substitutor) as ConstructorDescriptor

            return AddConstructorParametersFromSuperFix(delegator, classDeclaration, substitutedConstructor)
        }
    }
}