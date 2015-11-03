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
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.*

public enum class TypeNullability {
    NOT_NULL,
    NULLABLE,
    FLEXIBLE
}

public fun KotlinType.nullability(): TypeNullability {
    return when {
        isNullabilityFlexible() -> TypeNullability.FLEXIBLE
        TypeUtils.isNullableType(this) -> TypeNullability.NULLABLE
        else -> TypeNullability.NOT_NULL
    }
}

val KotlinType.builtIns: KotlinBuiltIns
    get() = constructor.builtIns

fun KotlinType.makeNullable() = TypeUtils.makeNullable(this)
fun KotlinType.makeNotNullable() = TypeUtils.makeNotNullable(this)

fun KotlinType.immediateSupertypes(): Collection<KotlinType> = TypeUtils.getImmediateSupertypes(this)
fun KotlinType.supertypes(): Collection<KotlinType> = TypeUtils.getAllSupertypes(this)

fun KotlinType.isNothing(): Boolean = KotlinBuiltIns.isNothing(this)
fun KotlinType.isUnit(): Boolean = KotlinBuiltIns.isUnit(this)
fun KotlinType.isAnyOrNullableAny(): Boolean = KotlinBuiltIns.isAnyOrNullableAny(this)
fun KotlinType.isBoolean(): Boolean = KotlinBuiltIns.isBoolean(this)
fun KotlinType.isBooleanOrNullableBoolean(): Boolean = KotlinBuiltIns.isBooleanOrNullableBoolean(this)

private fun KotlinType.getContainedTypeParameters(): Collection<TypeParameterDescriptor> {
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

public fun KotlinType.getContainedAndCapturedTypeParameterConstructors(): Collection<TypeConstructor> {
    // todo type arguments (instead of type parameters) of the type of outer class must be considered; KT-6325
    val capturedTypeParameters = getConstructor().getDeclarationDescriptor()?.getCapturedTypeParameters() ?: emptyList()
    val typeParameters = getContainedTypeParameters() + capturedTypeParameters
    return typeParameters.map { it.getTypeConstructor() }.toReadOnlyList()
}

public fun KotlinType.isSubtypeOf(superType: KotlinType): Boolean = KotlinTypeChecker.DEFAULT.isSubtypeOf(this, superType)

public fun KotlinType.cannotBeReified(): Boolean = KotlinBuiltIns.isNothingOrNullableNothing(this) || this.isDynamic()

fun TypeProjection.substitute(doSubstitute: (KotlinType) -> KotlinType): TypeProjection {
    return if (isStarProjection())
        this
    else TypeProjectionImpl(getProjectionKind(), doSubstitute(getType()))
}

fun KotlinType.replaceAnnotations(newAnnotations: Annotations): KotlinType {
    if (annotations.isEmpty() && newAnnotations.isEmpty()) return this
    return object : DelegatingType() {
        override fun getDelegate() = this@replaceAnnotations

        override fun getAnnotations() = newAnnotations
    }
}

public fun KotlinTypeChecker.equalTypesOrNulls(type1: KotlinType?, type2: KotlinType?): Boolean {
    if (type1 === type2) return true
    if (type1 == null || type2 == null) return false
    return equalTypes(type1, type2)
}

fun KotlinType.containsError() = ErrorUtils.containsErrorType(this)

public fun List<KotlinType>.defaultProjections(): List<TypeProjection> = map { TypeProjectionImpl(it) }

public fun KotlinType.isDefaultBound(): Boolean = KotlinBuiltIns.isDefaultBound(getSupertypeRepresentative())

public fun createProjection(type: KotlinType, projectionKind: Variance, typeParameterDescriptor: TypeParameterDescriptor?): TypeProjection =
        TypeProjectionImpl(if (typeParameterDescriptor?.variance == projectionKind) Variance.INVARIANT else projectionKind, type)

fun Collection<KotlinType>.closure(f: (KotlinType) -> Collection<KotlinType>): Collection<KotlinType> {
    if (size == 0) return this

    val result = HashSet(this)
    var elementsToCheck = result
    var oldSize = 0
    while (result.size > oldSize) {
        oldSize = result.size
        val toAdd = hashSetOf<KotlinType>()
        elementsToCheck.forEach { toAdd.addAll(f(it)) }
        result.addAll(toAdd)
        elementsToCheck = toAdd
    }

    return result
}

fun boundClosure(types: Collection<KotlinType>): Collection<KotlinType> =
        types.closure { type -> TypeUtils.getTypeParameterDescriptorOrNull(type)?.upperBounds ?: emptySet() }

fun constituentTypes(types: Collection<KotlinType>): Collection<KotlinType> {
    val result = hashSetOf<KotlinType>()
    constituentTypes(result, types)
    return result
}

private fun constituentTypes(result: MutableSet<KotlinType>, types: Collection<KotlinType>) {
    result.addAll(types)
    for (type in types) {
        if (type.isFlexible()) {
            with (type.flexibility()) { constituentTypes(result, setOf(lowerBound, upperBound)) }
        }
        else {
            constituentTypes(result, type.arguments.filterNot { it.isStarProjection }.map { it.type })
        }
    }
}
