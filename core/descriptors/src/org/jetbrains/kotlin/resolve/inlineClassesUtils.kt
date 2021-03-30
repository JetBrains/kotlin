/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

val JVM_INLINE_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmInline")

// FIXME: DeserializedClassDescriptor in reflection do not have @JvmInline annotation, that we
// FIXME: would like to check as well.
fun DeclarationDescriptor.isInlineClass(): Boolean = this is ClassDescriptor && (isInline || isValue)

fun KotlinType.unsubstitutedUnderlyingType(): KotlinType? =
    constructor.declarationDescriptor.safeAs<ClassDescriptor>()?.inlineClassRepresentation?.underlyingType

fun KotlinType.isInlineClassType(): Boolean = constructor.declarationDescriptor?.isInlineClass() ?: false

fun KotlinType.substitutedUnderlyingType(): KotlinType? =
    unsubstitutedUnderlyingType()?.let { TypeSubstitutor.create(this).substitute(it, Variance.INVARIANT) }

fun KotlinType.isRecursiveInlineClassType(): Boolean =
    isRecursiveInlineClassTypeInner(hashSetOf())

private fun KotlinType.isRecursiveInlineClassTypeInner(visited: HashSet<ClassifierDescriptor>): Boolean {
    val descriptor = constructor.declarationDescriptor?.original ?: return false

    if (!visited.add(descriptor)) return true

    return when (descriptor) {
        is ClassDescriptor ->
            descriptor.isInlineClass() &&
                    unsubstitutedUnderlyingType()?.isRecursiveInlineClassTypeInner(visited) == true

        is TypeParameterDescriptor ->
            descriptor.upperBounds.any { it.isRecursiveInlineClassTypeInner(visited) }

        else -> false
    }
}

fun KotlinType.isNullableUnderlyingType(): Boolean {
    if (!isInlineClassType()) return false
    val underlyingType = unsubstitutedUnderlyingType() ?: return false

    return TypeUtils.isNullableType(underlyingType)
}

fun CallableDescriptor.isGetterOfUnderlyingPropertyOfInlineClass() =
    this is PropertyGetterDescriptor && correspondingProperty.isUnderlyingPropertyOfInlineClass()

fun VariableDescriptor.isUnderlyingPropertyOfInlineClass(): Boolean =
    extensionReceiverParameter == null &&
            (containingDeclaration as? ClassDescriptor)?.inlineClassRepresentation?.underlyingPropertyName == this.name
