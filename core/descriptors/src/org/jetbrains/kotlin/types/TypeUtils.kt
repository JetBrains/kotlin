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

package org.jetbrains.kotlin.types.typeUtil

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.*

private fun JetType.getContainedTypeParameters(): Collection<TypeParameterDescriptor> {
    val declarationDescriptor = getConstructor().getDeclarationDescriptor()
    if (declarationDescriptor is TypeParameterDescriptor) return listOf(declarationDescriptor)

    val flexibility = getCapability(javaClass<Flexibility>())
    if (flexibility != null) {
        return flexibility.lowerBound.getContainedTypeParameters() + flexibility.upperBound.getContainedTypeParameters()
    }
    return getArguments().filter { !it.isStarProjection() }.map { it.getType() }.flatMap { it.getContainedTypeParameters() }
}

fun DeclarationDescriptor.getCapturedTypeParameters(): Collection<TypeParameterDescriptor> {
    val result = LinkedHashSet<TypeParameterDescriptor>()
    val containingDeclaration = this.getContainingDeclaration()

    if (containingDeclaration is ClassDescriptor) {
        result.addAll(containingDeclaration.getDefaultType().getContainedTypeParameters())
    }
    else if (containingDeclaration is CallableDescriptor) {
        result.addAll(containingDeclaration.getTypeParameters())
    }
    if (containingDeclaration != null) {
        result.addAll(containingDeclaration.getCapturedTypeParameters())
    }
    return result
}

public fun JetType.getContainedAndCapturedTypeParameterConstructors(): Collection<TypeConstructor> {
    // todo type arguments (instead of type parameters) of the type of outer class must be considered; KT-6325
    val capturedTypeParameters = getConstructor().getDeclarationDescriptor()?.getCapturedTypeParameters() ?: emptyList()
    val typeParameters = getContainedTypeParameters() + capturedTypeParameters
    return typeParameters.map { it.getTypeConstructor() }.toReadOnlyList()
}

public fun JetType.isSubtypeOf(superType: JetType): Boolean = JetTypeChecker.DEFAULT.isSubtypeOf(this, superType)

public fun JetType.cannotBeReified(): Boolean = KotlinBuiltIns.isNothingOrNullableNothing(this) || this.isDynamic()

fun TypeProjection.substitute(doSubstitute: (JetType) -> JetType): TypeProjection {
    return if (isStarProjection())
        this
    else TypeProjectionImpl(getProjectionKind(), doSubstitute(getType()))
}

fun JetType.replaceAnnotations(newAnnotations: Annotations): JetType {
    if (newAnnotations.isEmpty()) return this
    return object : DelegatingType() {
        override fun getDelegate() = this@replaceAnnotations

        override fun getAnnotations() = newAnnotations
    }
}

public fun JetTypeChecker.equalTypesOrNulls(type1: JetType?, type2: JetType?): Boolean {
    if (type1 identityEquals type2) return true
    if (type1 == null || type2 == null) return false
    return equalTypes(type1, type2)
}

fun JetType.getNestedArguments(): List<TypeProjection> {
    val result = ArrayList<TypeProjection>()

    val stack = ArrayDeque<TypeProjection>()
    stack.push(TypeProjectionImpl(this))

    while (!stack.isEmpty()) {
        val typeProjection = stack.pop()
        if (typeProjection.isStarProjection()) continue

        result.add(typeProjection)

        typeProjection.getType().getArguments().forEach { stack.add(it) }
    }
    return result
}