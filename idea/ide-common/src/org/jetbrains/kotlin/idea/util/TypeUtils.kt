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

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.JETBRAINS_READONLY_ANNOTATION
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.substitute

fun JetType.makeNullable() = TypeUtils.makeNullable(this)
fun JetType.makeNotNullable() = TypeUtils.makeNotNullable(this)

fun JetType.supertypes(): Set<JetType> = TypeUtils.getAllSupertypes(this)

fun JetType.isNothing(): Boolean = KotlinBuiltIns.isNothing(this)
fun JetType.isUnit(): Boolean = KotlinBuiltIns.isUnit(this)
fun JetType.isAny(): Boolean = KotlinBuiltIns.isAnyOrNullableAny(this)

public fun approximateFlexibleTypes(jetType: JetType, outermost: Boolean = true): JetType {
    if (jetType.isDynamic()) return jetType
    if (jetType.isFlexible()) {
        val flexible = jetType.flexibility()
        val lowerClass = flexible.lowerBound.getConstructor().getDeclarationDescriptor() as? ClassDescriptor?
        val isCollection = lowerClass != null && JavaToKotlinClassMap.INSTANCE.isMutableCollection(lowerClass)
        // (Mutable)Collection<T>! -> MutableCollection<T>?
        // Foo<(Mutable)Collection<T>!>! -> Foo<Collection<T>>?
        // Foo! -> Foo?
        // Foo<Bar!>! -> Foo<Bar>?
        var approximation =
                if (isCollection)
                    TypeUtils.makeNullableAsSpecified(if (jetType.isMarkedReadOnly()) flexible.upperBound else flexible.lowerBound, outermost)
                else
                    if (outermost) flexible.upperBound else flexible.lowerBound

        approximation = approximateFlexibleTypes(approximation)

        approximation = if (jetType.isMarkedNotNull()) approximation.makeNotNullable() else approximation

        if (approximation.isMarkedNullable() && !flexible.lowerBound.isMarkedNullable() && TypeUtils.isTypeParameter(approximation) && TypeUtils.hasNullableSuperType(approximation)) {
            approximation = approximation.makeNotNullable()
        }

        return approximation
    }
    return JetTypeImpl(
            jetType.getAnnotations(),
            jetType.getConstructor(),
            jetType.isMarkedNullable(),
            jetType.getArguments().map { it.substitute { type -> approximateFlexibleTypes(type, false)} },
            ErrorUtils.createErrorScope("This type is not supposed to be used in member resolution", true)
    )
}

private fun JetType.isMarkedReadOnly() = hasAnnotationMaybeExternal(JETBRAINS_READONLY_ANNOTATION)
private fun JetType.isMarkedNotNull() = hasAnnotationMaybeExternal(JETBRAINS_NOT_NULL_ANNOTATION)

private fun JetType.hasAnnotationMaybeExternal(fqName: FqName) = with (getAnnotations()) {
    findAnnotation(fqName) ?: findExternalAnnotation(fqName)
} != null

public enum class TypeNullability {
    NOT_NULL
    NULLABLE
    FLEXIBLE
}

public fun JetType.nullability(): TypeNullability {
    return when {
        isNullabilityFlexible() -> TypeNullability.FLEXIBLE
        TypeUtils.isNullableType(this) -> TypeNullability.NULLABLE
        else -> TypeNullability.NOT_NULL
    }
}

fun JetType.isResolvableInScope(scope: JetScope?, checkTypeParameters: Boolean): Boolean {
    if (canBeReferencedViaImport()) return true

    val descriptor = getConstructor().getDeclarationDescriptor()
    if (descriptor == null || descriptor.getName().isSpecial()) return false
    if (!checkTypeParameters && descriptor is TypeParameterDescriptor) return true

    return scope != null && scope.getClassifier(descriptor.getName()) == descriptor
}

public fun JetType.approximateWithResolvableType(scope: JetScope?, checkTypeParameters: Boolean): JetType {
    if (isError() || isResolvableInScope(scope, checkTypeParameters)) return this
    return supertypes().firstOrNull { it.isResolvableInScope(scope, checkTypeParameters) }
           ?: KotlinBuiltIns.getInstance().getAnyType()
}