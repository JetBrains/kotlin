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

import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.resolve.jvm.kotlinSignature.CollectionClassMapping
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import java.util.LinkedHashSet
import org.jetbrains.kotlin.types.typeUtil.substitute

fun JetType.makeNullable() = TypeUtils.makeNullable(this)
fun JetType.makeNotNullable() = TypeUtils.makeNotNullable(this)

fun JetType.supertypes(): Set<JetType> = TypeUtils.getAllSupertypes(this)

fun JetType.isUnit(): Boolean = KotlinBuiltIns.isUnit(this)
fun JetType.isAny(): Boolean = KotlinBuiltIns.isAnyOrNullableAny(this)

public fun approximateFlexibleTypes(jetType: JetType, outermost: Boolean = true): JetType {
    if (jetType.isDynamic()) return jetType
    if (jetType.isFlexible()) {
        val flexible = jetType.flexibility()
        val lowerClass = flexible.lowerBound.getConstructor().getDeclarationDescriptor() as? ClassDescriptor?
        val isCollection = lowerClass != null && CollectionClassMapping.getInstance().isMutableCollection(lowerClass)
        // (Mutable)Collection<T>! -> MutableCollection<T>?
        // Foo<(Mutable)Collection<T>!>! -> Foo<Collection<T>>?
        // Foo! -> Foo?
        // Foo<Bar!>! -> Foo<Bar>?
        val approximation =
                if (isCollection)
                    TypeUtils.makeNullableAsSpecified(if (jetType.isMarkedReadOnly()) flexible.upperBound else flexible.lowerBound, outermost)
                else
                    if (outermost) flexible.upperBound else flexible.lowerBound
        val approximated = approximateFlexibleTypes(approximation)
        return if (jetType.isMarkedNotNull()) approximated.makeNotNullable() else approximated
    }
    return JetTypeImpl(
            jetType.getAnnotations(),
            jetType.getConstructor(),
            jetType.isMarkedNullable(),
            jetType.getArguments().map { it.substitute { type -> approximateFlexibleTypes(type, false)} },
            ErrorUtils.createErrorScope("This type is not supposed to be used in member resolution", true)
    )
}

private fun JetType.isMarkedReadOnly() = getAnnotations().findAnnotation(JvmAnnotationNames.JETBRAINS_READONLY_ANNOTATION) != null
private fun JetType.isMarkedNotNull() = getAnnotations().findAnnotation(JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION) != null

public fun JetType.getAllReferencedTypes(): Set<JetType> {
    val types = LinkedHashSet<JetType>()

    fun addType(type: JetType) {
        types.add(type)
        type.getArguments().forEach { addType(it.getType()) }
    }

    addType(this)
    return types
}

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
