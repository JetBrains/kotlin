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

import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.MissingDependencyErrorClass
import java.util.*

fun ClassDescriptor.computeConstructorTypeParameters(): List<TypeParameterDescriptor> {
    val declaredParameters = declaredTypeParameters

    if (!isInner) return declaredParameters
    val containingTypeConstructor = (containingDeclaration as? ClassDescriptor)?.typeConstructor ?: return emptyList()

    val additional = containingTypeConstructor.parameters.map { it.capturedCopyForInnerDeclaration(this, declaredParameters.size) }
    if (additional.isEmpty()) return declaredParameters

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
    if (classDescriptor == null) return null

    val toIndex = classDescriptor.declaredTypeParameters.size + index
    val argumentsSubList = arguments.subList(index, toIndex)

    if (!classDescriptor.isInner) {
        assert(toIndex == arguments.size) { "${arguments.size - toIndex} trailing arguments were found in $this type" }

        return PossiblyInnerType(classDescriptor, argumentsSubList, null)
    }

    return PossiblyInnerType(
            classDescriptor, argumentsSubList,
            buildPossiblyInnerType(classDescriptor.containingDeclaration as? ClassDescriptor, toIndex))
}
