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

import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KtPsiClassWrapper
import org.jetbrains.kotlin.idea.refactoring.memberInfo.getClassDescriptorIfAny
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.utils.collectDescriptorsFiltered
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.substitutions.getTypeSubstitution
import org.jetbrains.kotlin.utils.keysToMap
import java.util.*

class KotlinPullUpData(val sourceClass: KtClassOrObject,
                       val targetClass: PsiNamedElement,
                       val membersToMove: Collection<KtNamedDeclaration>) {
    val resolutionFacade = sourceClass.getResolutionFacade()

    val sourceClassContext = resolutionFacade.analyzeFullyAndGetResult(listOf(sourceClass)).bindingContext

    val sourceClassDescriptor = sourceClassContext[BindingContext.DECLARATION_TO_DESCRIPTOR, sourceClass] as ClassDescriptor

    val memberDescriptors = membersToMove.keysToMap {
        when (it) {
            is KtPsiClassWrapper -> it.psiClass.getJavaClassDescriptor(resolutionFacade)!!
            is KtParameter -> sourceClassContext[BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, it]!!
            else -> sourceClassContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it]!!
        }
    }

    val targetClassDescriptor = targetClass.getClassDescriptorIfAny(resolutionFacade)!!

    val superEntryForTargetClass = sourceClass.getSuperTypeEntryByDescriptor(targetClassDescriptor, sourceClassContext)

    val targetClassSuperResolvedCall = superEntryForTargetClass.getResolvedCall(sourceClassContext)

    private val typeParametersInSourceClassContext by lazy {
        sourceClassDescriptor.declaredTypeParameters +
        sourceClass.getResolutionScope(sourceClassContext, resolutionFacade)
                .collectDescriptorsFiltered(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS)
                .filterIsInstance<TypeParameterDescriptor>()
    }

    val sourceToTargetClassSubstitutor: TypeSubstitutor by lazy {
        val substitution = LinkedHashMap<TypeConstructor, TypeProjection>()

        typeParametersInSourceClassContext.forEach {
            substitution[it.typeConstructor] = TypeProjectionImpl(TypeIntersector.getUpperBoundsAsType(it))
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

    val isInterfaceTarget: Boolean = targetClassDescriptor.kind == ClassKind.INTERFACE
}
