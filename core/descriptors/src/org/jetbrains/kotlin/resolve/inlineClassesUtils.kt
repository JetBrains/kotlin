/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun ClassDescriptor.underlyingRepresentation(): ValueParameterDescriptor? {
    if (!isInline) return null
    return unsubstitutedPrimaryConstructor?.valueParameters?.singleOrNull()
}

fun DeclarationDescriptor.isInlineClass() = this is ClassDescriptor && this.isInline

fun KotlinType.unsubstitutedUnderlyingParameter(): ValueParameterDescriptor? {
    return constructor.declarationDescriptor.safeAs<ClassDescriptor>()?.underlyingRepresentation()
}

fun KotlinType.unsubstitutedUnderlyingType(): KotlinType? = unsubstitutedUnderlyingParameter()?.type

fun KotlinType.isInlineClassType(): Boolean = constructor.declarationDescriptor?.isInlineClass() ?: false

fun KotlinType.substitutedUnderlyingType(): KotlinType? {
    val parameter = unsubstitutedUnderlyingParameter() ?: return null
    return memberScope.getContributedVariables(parameter.name, NoLookupLocation.FOR_ALREADY_TRACKED).singleOrNull()?.type
}

fun KotlinType.isRecursiveInlineClassType() =
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

fun PropertyDescriptor.isUnderlyingPropertyOfInlineClass(): Boolean {
    val containingDeclaration = this.containingDeclaration
    if (!containingDeclaration.isInlineClass()) return false

    return (containingDeclaration as ClassDescriptor).underlyingRepresentation()?.name == this.name
}
