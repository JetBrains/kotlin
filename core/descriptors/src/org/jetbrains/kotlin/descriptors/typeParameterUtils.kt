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

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.MissingDependencyErrorClass
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

fun ClassDescriptor.computeConstructorTypeParameters(): List<TypeParameterDescriptor> {
    val declaredParameters = declaredTypeParameters

    if (!isInner && containingDeclaration !is CallableDescriptor) return declaredParameters

    val parametersFromContainingFunctions =
            parents.takeWhile { it is CallableDescriptor }
                   .flatMap { (it as CallableDescriptor).typeParameters.asSequence() }.toList()

    val containingClassTypeConstructorParameters = parents.firstIsInstanceOrNull<ClassDescriptor>()?.typeConstructor?.parameters.orEmpty()
    if (parametersFromContainingFunctions.isEmpty() && containingClassTypeConstructorParameters.isEmpty()) return declaredTypeParameters

    val additional =
            (parametersFromContainingFunctions + containingClassTypeConstructorParameters)
                .map { it.capturedCopyForInnerDeclaration(this, declaredParameters.size) }

    return declaredParameters + additional
}

private fun TypeParameterDescriptor.capturedCopyForInnerDeclaration(
        declarationDescriptor: DeclarationDescriptor,
        declaredTypeParametersCount: Int
) = CapturedTypeParameterDescriptor(this, declarationDescriptor, declaredTypeParametersCount)

private class CapturedTypeParameterDescriptor(
        private val originalDescriptor: TypeParameterDescriptor,
        private val declarationDescriptor: DeclarationDescriptor,
        private val declaredTypeParametersCount: Int
) : TypeParameterDescriptor by originalDescriptor {
    override fun isCapturedFromOuterDeclaration() = true
    override fun getOriginal() = originalDescriptor.original
    override fun getContainingDeclaration() = declarationDescriptor
    override fun getIndex() = declaredTypeParametersCount + originalDescriptor.index
    override fun toString() = originalDescriptor.toString() + "[inner-copy]"
}

class PossiblyInnerType(
        val classDescriptor: ClassDescriptor,
        val arguments: List<TypeProjection>,
        val outerType: PossiblyInnerType?) {
    fun segments(): List<PossiblyInnerType> = outerType?.segments().orEmpty() + this
}

fun KotlinType.buildPossiblyInnerType(): PossiblyInnerType? {
    if (constructor.declarationDescriptor is MissingDependencyErrorClass) {
        return getCapability<PossiblyInnerTypeCapability>()?.possiblyInnerType
    }

    return buildPossiblyInnerType(constructor.declarationDescriptor as? ClassDescriptor, 0)
}

private fun KotlinType.buildPossiblyInnerType(classDescriptor: ClassDescriptor?, index: Int): PossiblyInnerType? {
    if (classDescriptor == null || ErrorUtils.isError(classDescriptor)) return null

    val toIndex = classDescriptor.declaredTypeParameters.size + index
    if (!classDescriptor.isInner) {
        assert(toIndex == arguments.size || DescriptorUtils.isLocal(classDescriptor)) {
            "${arguments.size - toIndex} trailing arguments were found in $this type"
        }

        return PossiblyInnerType(classDescriptor, arguments.subList(index, arguments.size), null)
    }

    val argumentsSubList = arguments.subList(index, toIndex)
    return PossiblyInnerType(
            classDescriptor, argumentsSubList,
            buildPossiblyInnerType(classDescriptor.containingDeclaration as? ClassDescriptor, toIndex))
}
