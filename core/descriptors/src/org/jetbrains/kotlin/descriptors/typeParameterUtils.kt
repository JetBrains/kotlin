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
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

fun ClassifierDescriptorWithTypeParameters.computeConstructorTypeParameters(): List<TypeParameterDescriptor> {
    val declaredParameters = declaredTypeParameters

    if (!isInner && containingDeclaration !is CallableDescriptor) return declaredParameters

    val parametersFromContainingFunctions =
        parents.takeWhile { it is CallableDescriptor }
            .filter { it !is ConstructorDescriptor }
            .flatMap { (it as CallableDescriptor).typeParameters.asSequence() }
            .toList()

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
    override fun toString() = "$originalDescriptor[inner-copy]"
}

class PossiblyInnerType(
    val classifierDescriptor: ClassifierDescriptorWithTypeParameters,
    val arguments: List<TypeProjection>,
    val outerType: PossiblyInnerType?
) {
    val classDescriptor: ClassDescriptor
        get() = classifierDescriptor as ClassDescriptor

    fun segments(): List<PossiblyInnerType> = outerType?.segments().orEmpty() + this
}

fun KotlinType.buildPossiblyInnerType(): PossiblyInnerType? {
    return buildPossiblyInnerType(constructor.declarationDescriptor as? ClassifierDescriptorWithTypeParameters, 0)
}

private fun KotlinType.buildPossiblyInnerType(
    classifierDescriptor: ClassifierDescriptorWithTypeParameters?,
    index: Int
): PossiblyInnerType? {
    if (classifierDescriptor == null || ErrorUtils.isError(classifierDescriptor)) return null

    val toIndex = classifierDescriptor.declaredTypeParameters.size + index
    if (!classifierDescriptor.isInner) {
        assert(toIndex == arguments.size || DescriptorUtils.isLocal(classifierDescriptor)) {
            "${arguments.size - toIndex} trailing arguments were found in $this type"
        }

        return PossiblyInnerType(classifierDescriptor, arguments.subList(index, arguments.size), null)
    }

    val argumentsSubList = arguments.subList(index, toIndex)
    return PossiblyInnerType(
        classifierDescriptor, argumentsSubList,
        buildPossiblyInnerType(classifierDescriptor.containingDeclaration as? ClassifierDescriptorWithTypeParameters, toIndex)
    )
}
