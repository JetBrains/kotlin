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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.getResolutionScope
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.substitutions.getTypeSubstitution
import org.jetbrains.kotlin.utils.keysToMap
import java.util.LinkedHashMap

class KotlinPullUpData(val sourceClass: JetClassOrObject,
                       val targetClass: JetClass,
                       val membersToMove: Collection<JetNamedDeclaration>) {
    val resolutionFacade = sourceClass.getResolutionFacade()

    val sourceClassContext = resolutionFacade.analyzeFullyAndGetResult(listOf(sourceClass)).bindingContext

    val sourceClassDescriptor = sourceClassContext[BindingContext.DECLARATION_TO_DESCRIPTOR, sourceClass] as ClassDescriptor

    val memberDescriptors = membersToMove.keysToMap { sourceClassContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it]!! }

    val targetClassDescriptor = resolutionFacade.resolveToDescriptor(targetClass) as ClassDescriptor

    val typeParametersInSourceClassContext by lazy {
        sourceClassDescriptor.typeConstructor.parameters + sourceClass.getResolutionScope(sourceClassContext, resolutionFacade)
                .getDescriptors(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS)
                .filterIsInstance<TypeParameterDescriptor>()
    }

    val sourceToTargetClassSubstitutor: TypeSubstitutor by lazy {
        val substitution = LinkedHashMap<TypeConstructor, TypeProjection>()

        typeParametersInSourceClassContext.forEach {
            substitution[it.typeConstructor] = TypeProjectionImpl(it.upperBoundsAsType)
        }

        val superClassSubstitution = getTypeSubstitution(targetClassDescriptor.defaultType, sourceClassDescriptor.defaultType)
                                     ?: emptyMap<TypeConstructor, TypeProjection>()
        for ((typeConstructor, typeProjection) in superClassSubstitution) {
            val subClassTypeParameter = typeProjection.type.constructor.declarationDescriptor as? TypeParameterDescriptor
                                        ?: continue
            val superClassTypeParameter = typeConstructor.declarationDescriptor
                                          ?: continue
            substitution[subClassTypeParameter.typeConstructor] = TypeProjectionImpl(superClassTypeParameter.defaultType)
        }

        TypeSubstitutor.create(substitution)
    }
}