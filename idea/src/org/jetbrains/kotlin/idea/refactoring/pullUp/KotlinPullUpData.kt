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

package org.jetbrains.kotlin.idea.refactoring.pullUp

import com.intellij.refactoring.memberPullUp.PullUpData
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.getResolutionScope
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.substitutions.getTypeSubstitution
import org.jetbrains.kotlin.utils.keysToMap
import java.util.*

class KotlinPullUpData(val sourceClass: JetClassOrObject,
                       val targetClass: JetClass,
                       val membersToMove: Collection<JetNamedDeclaration>) {
    val resolutionFacade = sourceClass.getResolutionFacade()

    val sourceClassContext = resolutionFacade.analyzeFullyAndGetResult(listOf(sourceClass)).bindingContext

    val sourceClassDescriptor = sourceClassContext[BindingContext.DECLARATION_TO_DESCRIPTOR, sourceClass] as ClassDescriptor

    val memberDescriptors = membersToMove.keysToMap { sourceClassContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it]!! }

    val targetClassDescriptor = resolutionFacade.resolveToDescriptor(targetClass) as ClassDescriptor

    val typeParametersInSourceClassContext by lazy {
        sourceClassDescriptor.getTypeConstructor().getParameters() + sourceClass.getResolutionScope(sourceClassContext, resolutionFacade)
                .getDescriptors(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS)
                .filterIsInstance<TypeParameterDescriptor>()
    }

    val sourceToTargetClassSubstitutor: TypeSubstitutor by lazy {
        val substitution = LinkedHashMap<TypeConstructor, TypeProjection>()

        typeParametersInSourceClassContext.forEach {
            substitution[it.getTypeConstructor()] = TypeProjectionImpl(it.getUpperBoundsAsType())
        }

        val superClassSubstitution = getTypeSubstitution(targetClassDescriptor.getDefaultType(), sourceClassDescriptor.getDefaultType())
                                     ?: emptyMap<TypeConstructor, TypeProjection>()
        for ((typeConstructor, typeProjection) in superClassSubstitution) {
            val subClassTypeParameter = typeProjection.getType().getConstructor().getDeclarationDescriptor() as? TypeParameterDescriptor
                                        ?: continue
            val superClassTypeParameter = typeConstructor.getDeclarationDescriptor()
                                          ?: continue
            substitution[subClassTypeParameter.getTypeConstructor()] = TypeProjectionImpl(superClassTypeParameter.getDefaultType())
        }

        TypeSubstitutor.create(substitution)
    }
}