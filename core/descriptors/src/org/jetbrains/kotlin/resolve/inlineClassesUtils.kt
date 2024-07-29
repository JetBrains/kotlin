/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.inlineClassRepresentation
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.isNullableType

val JVM_INLINE_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmInline")
val JVM_INLINE_ANNOTATION_CLASS_ID = ClassId.topLevel(JVM_INLINE_ANNOTATION_FQ_NAME)

// FIXME: DeserializedClassDescriptor in reflection do not have @JvmInline annotation, that we
// FIXME: would like to check as well.
fun DeclarationDescriptor.isInlineClass(): Boolean = this is ClassDescriptor && this.valueClassRepresentation is InlineClassRepresentation

fun DeclarationDescriptor.isMultiFieldValueClass(): Boolean =
    this is ClassDescriptor && this.valueClassRepresentation is MultiFieldValueClassRepresentation

fun DeclarationDescriptor.isValueClass(): Boolean = isInlineClass() || isMultiFieldValueClass()

fun DeclarationDescriptor.unsubstitutedUnderlyingType(): KotlinType? =
    (this as? ClassDescriptor)?.inlineClassRepresentation?.underlyingType

fun DeclarationDescriptor.unsubstitutedUnderlyingTypes(): List<KotlinType> {
    return when {
        this !is ClassDescriptor -> emptyList()
        isInlineClass() -> listOfNotNull(unsubstitutedUnderlyingType())
        isMultiFieldValueClass() -> unsubstitutedPrimaryConstructor?.valueParameters?.map { it.type } ?: emptyList()
        else -> emptyList()
    }
}


fun KotlinType.isInlineClassType(): Boolean = constructor.declarationDescriptor?.isInlineClass() ?: false
fun KotlinType.isValueClassType(): Boolean = constructor.declarationDescriptor?.isValueClass() ?: false

fun KotlinType.needsMfvcFlattening(): Boolean =
    constructor.declarationDescriptor?.run { isMultiFieldValueClass() && !isNullableType() } == true

fun KotlinType.substitutedUnderlyingType(): KotlinType? =
    constructor.declarationDescriptor?.unsubstitutedUnderlyingType()
        ?.let { TypeSubstitutor.create(this).substitute(it, Variance.INVARIANT) }

fun KotlinType.isRecursiveInlineOrValueClassType(): Boolean =
    isRecursiveInlineOrValueClassTypeInner(hashSetOf())

private fun KotlinType.isRecursiveInlineOrValueClassTypeInner(visited: HashSet<ClassifierDescriptor>): Boolean {
    val types = when (val descriptor = constructor.declarationDescriptor?.original?.takeIf { it.isValueClass() }) {
        is ClassDescriptor -> if (descriptor.isValueClass()) descriptor.unsubstitutedUnderlyingTypes() else emptyList()
        is TypeParameterDescriptor -> descriptor.upperBounds
        else -> emptyList()
    }
    return types.any {
        val classifier = it.constructor.declarationDescriptor?.original ?: return@any false
        !visited.add(classifier) || it.isRecursiveInlineOrValueClassTypeInner(visited).also { visited.remove(classifier) }
    }
}

fun KotlinType.isNullableUnderlyingType(): Boolean {
    if (!isInlineClassType()) return false
    val underlyingType = constructor.declarationDescriptor?.unsubstitutedUnderlyingType() ?: return false

    return TypeUtils.isNullableType(underlyingType)
}

fun CallableDescriptor.isGetterOfUnderlyingPropertyOfValueClass() =
    this is PropertyGetterDescriptor && correspondingProperty.isUnderlyingPropertyOfValueClass()

fun VariableDescriptor.isUnderlyingPropertyOfInlineClass(): Boolean =
    extensionReceiverParameter == null &&
            (containingDeclaration as? ClassDescriptor)?.inlineClassRepresentation?.underlyingPropertyName == this.name

fun VariableDescriptor.isUnderlyingPropertyOfValueClass(): Boolean =
    extensionReceiverParameter == null &&
            (containingDeclaration as? ClassDescriptor)?.valueClassRepresentation?.containsPropertyWithName(this.name) == true
