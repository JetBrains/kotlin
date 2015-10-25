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

@file:JvmName("TypeUtils")

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.JETBRAINS_READONLY_ANNOTATION
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*

public fun approximateFlexibleTypes(jetType: KotlinType, outermost: Boolean = true): KotlinType {
    if (jetType.isDynamic()) return jetType
    if (jetType.isFlexible()) {
        val flexible = jetType.flexibility()
        val lowerClass = flexible.lowerBound.getConstructor().getDeclarationDescriptor() as? ClassDescriptor?
        val isCollection = lowerClass != null && JavaToKotlinClassMap.INSTANCE.isMutable(lowerClass)
        // (Mutable)Collection<T>! -> MutableCollection<T>?
        // Foo<(Mutable)Collection<T>!>! -> Foo<Collection<T>>?
        // Foo! -> Foo?
        // Foo<Bar!>! -> Foo<Bar>?
        var approximation =
                if (isCollection)
                    TypeUtils.makeNullableAsSpecified(if (jetType.isAnnotatedReadOnly()) flexible.upperBound else flexible.lowerBound, outermost)
                else
                    if (outermost) flexible.upperBound else flexible.lowerBound

        approximation = approximateFlexibleTypes(approximation)

        approximation = if (jetType.isAnnotatedNotNull()) approximation.makeNotNullable() else approximation

        if (approximation.isMarkedNullable() && !flexible.lowerBound.isMarkedNullable() && TypeUtils.isTypeParameter(approximation) && TypeUtils.hasNullableSuperType(approximation)) {
            approximation = approximation.makeNotNullable()
        }

        return approximation
    }
    return KotlinTypeImpl.create(
            jetType.getAnnotations(),
            jetType.getConstructor(),
            jetType.isMarkedNullable(),
            jetType.getArguments().map { it.substitute { type -> approximateFlexibleTypes(type, false)} },
            ErrorUtils.createErrorScope("This type is not supposed to be used in member resolution", true)
    )
}

public fun KotlinType.isAnnotatedReadOnly(): Boolean = hasAnnotationMaybeExternal(JETBRAINS_READONLY_ANNOTATION)
public fun KotlinType.isAnnotatedNotNull(): Boolean = hasAnnotationMaybeExternal(JETBRAINS_NOT_NULL_ANNOTATION)
public fun KotlinType.isAnnotatedNullable(): Boolean = hasAnnotationMaybeExternal(JETBRAINS_NULLABLE_ANNOTATION)

private fun KotlinType.hasAnnotationMaybeExternal(fqName: FqName) = with (getAnnotations()) {
    findAnnotation(fqName) ?: findExternalAnnotation(fqName)
} != null

fun KotlinType.isResolvableInScope(scope: LexicalScope?, checkTypeParameters: Boolean): Boolean {
    if (canBeReferencedViaImport()) return true

    val descriptor = getConstructor().getDeclarationDescriptor()
    if (descriptor == null || descriptor.getName().isSpecial()) return false
    if (!checkTypeParameters && descriptor is TypeParameterDescriptor) return true

    return scope != null && scope.findClassifier(descriptor.name, NoLookupLocation.FROM_IDE) == descriptor
}

public fun KotlinType.approximateWithResolvableType(scope: LexicalScope?, checkTypeParameters: Boolean): KotlinType {
    if (isError() || isResolvableInScope(scope, checkTypeParameters)) return this
    return supertypes().firstOrNull { it.isResolvableInScope(scope, checkTypeParameters) }
           ?: builtIns.anyType
}

public fun KotlinType.anonymousObjectSuperTypeOrNull(): KotlinType? {
    val classDescriptor = constructor.declarationDescriptor
    if (classDescriptor != null && DescriptorUtils.isAnonymousObject(classDescriptor)) {
        return immediateSupertypes().firstOrNull() ?: classDescriptor.builtIns.anyType
    }
    return null
}