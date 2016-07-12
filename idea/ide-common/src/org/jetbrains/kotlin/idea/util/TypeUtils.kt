/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

@file:JvmName("TypeUtils")

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*

fun KotlinType.approximateFlexibleTypes(preferNotNull: Boolean = false): KotlinType {
    if (isDynamic()) return this
    return approximateNonDynamicFlexibleTypes(preferNotNull)
}

private fun KotlinType.approximateNonDynamicFlexibleTypes(preferNotNull: Boolean = false): SimpleType {
    if (isFlexible()) {
        val flexible = asFlexibleType()
        val lowerClass = flexible.lowerBound.constructor.declarationDescriptor as? ClassDescriptor?
        val isCollection = lowerClass != null && JavaToKotlinClassMap.INSTANCE.isMutable(lowerClass)
        // (Mutable)Collection<T>! -> MutableCollection<T>?
        // Foo<(Mutable)Collection<T>!>! -> Foo<Collection<T>>?
        // Foo! -> Foo?
        // Foo<Bar!>! -> Foo<Bar>?
        var approximation =
                if (isCollection)
                    (if (isAnnotatedReadOnly()) flexible.upperBound else flexible.lowerBound).makeNullableAsSpecified(!preferNotNull)
                else
                    if (preferNotNull) flexible.lowerBound else flexible.upperBound

        approximation = approximation.approximateNonDynamicFlexibleTypes()

        approximation = if (isAnnotatedNotNull()) approximation.makeNullableAsSpecified(false) else approximation

        if (approximation.isMarkedNullable && !flexible.lowerBound.isMarkedNullable && TypeUtils.isTypeParameter(approximation) && TypeUtils.hasNullableSuperType(approximation)) {
            approximation = approximation.makeNullableAsSpecified(false)
        }

        return approximation
    }
    return KotlinTypeFactory.simpleType(annotations,
                                        constructor,
                                        arguments.map { it.substitute { type -> type.approximateFlexibleTypes(preferNotNull = true) } },
                                        isMarkedNullable,
                                        ErrorUtils.createErrorScope("This type is not supposed to be used in member resolution", true)
    )
}

fun KotlinType.isAnnotatedReadOnly(): Boolean = hasAnnotationMaybeExternal(JETBRAINS_READONLY_ANNOTATION)
fun KotlinType.isAnnotatedNotNull(): Boolean = hasAnnotationMaybeExternal(JETBRAINS_NOT_NULL_ANNOTATION)
fun KotlinType.isAnnotatedNullable(): Boolean = hasAnnotationMaybeExternal(JETBRAINS_NULLABLE_ANNOTATION)

private fun KotlinType.hasAnnotationMaybeExternal(fqName: FqName) = with (annotations) {
    findAnnotation(fqName) ?: findExternalAnnotation(fqName)
} != null

fun KotlinType.isResolvableInScope(scope: LexicalScope?, checkTypeParameters: Boolean): Boolean {
    if (canBeReferencedViaImport()) return true

    val descriptor = constructor.declarationDescriptor
    if (descriptor == null || descriptor.name.isSpecial) return false
    if (!checkTypeParameters && descriptor is TypeParameterDescriptor) return true

    return scope != null && scope.findClassifier(descriptor.name, NoLookupLocation.FROM_IDE) == descriptor
}

fun KotlinType.approximateWithResolvableType(scope: LexicalScope?, checkTypeParameters: Boolean): KotlinType {
    if (isError || isResolvableInScope(scope, checkTypeParameters)) return this
    return supertypes().firstOrNull { it.isResolvableInScope(scope, checkTypeParameters) }
           ?: builtIns.anyType
}

fun KotlinType.anonymousObjectSuperTypeOrNull(): KotlinType? {
    val classDescriptor = constructor.declarationDescriptor
    if (classDescriptor != null && DescriptorUtils.isAnonymousObject(classDescriptor)) {
        return immediateSupertypes().firstOrNull() ?: classDescriptor.builtIns.anyType
    }
    return null
}