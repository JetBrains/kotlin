/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.util

import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.types.JetTypeImpl
import org.jetbrains.jet.lang.types.TypeProjectionImpl
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.resolve.java.kotlinSignature.CollectionClassMapping
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames
import org.jetbrains.jet.lang.types.isFlexible
import org.jetbrains.jet.lang.types.flexibility
import java.util.ArrayList
import java.util.LinkedHashSet

fun JetType.makeNullable() = TypeUtils.makeNullable(this)
fun JetType.makeNotNullable() = TypeUtils.makeNotNullable(this)

fun JetType.supertypes(): Set<JetType> = TypeUtils.getAllSupertypes(this)

fun JetType.isUnit(): Boolean = KotlinBuiltIns.getInstance().isUnit(this)

public fun approximateFlexibleTypes(jetType: JetType, outermost: Boolean = true): JetType {
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
            jetType.isNullable(),
            jetType.getArguments().map { TypeProjectionImpl(it.getProjectionKind(), approximateFlexibleTypes(it.getType(), false)) },
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