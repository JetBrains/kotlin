/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.inlineClassRepresentation
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.isNullableType

val JVM_INLINE_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmInline")
val JVM_INLINE_ANNOTATION_CLASS_ID = ClassId.topLevel(JVM_INLINE_ANNOTATION_FQ_NAME)

val JVM_EXPOSE_BOXED_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.JvmExposeBoxed")
val JVM_EXPOSE_BOXED_ANNOTATION_CLASS_ID = ClassId.topLevel(JVM_EXPOSE_BOXED_ANNOTATION_FQ_NAME)

// FIXME: DeserializedClassDescriptor in reflection do not have @JvmInline annotation, that we
// FIXME: would like to check as well.
fun DeclarationDescriptor.isInlineClass(): Boolean = this is ClassDescriptor && this.valueClassRepresentation is InlineClassRepresentation

fun DeclarationDescriptor.isMultiFieldValueClass(): Boolean =
    this is ClassDescriptor && this.valueClassRepresentation is MultiFieldValueClassRepresentation

fun DeclarationDescriptor.isValueClass(): Boolean = isInlineClass() || isMultiFieldValueClass()

fun KotlinType.unsubstitutedUnderlyingType(): KotlinType? =
    (constructor.declarationDescriptor as? ClassDescriptor)?.inlineClassRepresentation?.underlyingType

fun KotlinType.unsubstitutedUnderlyingTypes(): List<KotlinType> {
    val declarationDescriptor = constructor.declarationDescriptor as? ClassDescriptor ?: return emptyList()
    return when {
        declarationDescriptor.isInlineClass() -> listOfNotNull(unsubstitutedUnderlyingType())
        declarationDescriptor.isMultiFieldValueClass() ->
            declarationDescriptor.unsubstitutedPrimaryConstructor?.valueParameters?.map { it.type } ?: emptyList()
        else -> emptyList()
    }
}


fun KotlinType.isInlineClassType(): Boolean = constructor.declarationDescriptor?.isInlineClass() ?: false

fun KotlinType.needsMfvcFlattening(): Boolean =
    constructor.declarationDescriptor?.run { isMultiFieldValueClass() && !isNullableType() } == true

fun KotlinType.substitutedUnderlyingType(): KotlinType? =
    unsubstitutedUnderlyingType()?.let { TypeSubstitutor.create(this).substitute(it, Variance.INVARIANT) }

fun KotlinType.substitutedUnderlyingTypes(): List<KotlinType?> =
    unsubstitutedUnderlyingTypes().map { TypeSubstitutor.create(this).substitute(it, Variance.INVARIANT) }

fun KotlinType.isRecursiveInlineOrValueClassType(): Boolean =
    isRecursiveInlineOrValueClassTypeInner(hashSetOf())

private fun KotlinType.isRecursiveInlineOrValueClassTypeInner(visited: HashSet<ClassifierDescriptor>): Boolean {
    val types = when (val descriptor = constructor.declarationDescriptor?.original?.takeIf { it.isValueClass() }) {
        is ClassDescriptor -> if (descriptor.isValueClass()) unsubstitutedUnderlyingTypes() else emptyList()
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
    val underlyingType = unsubstitutedUnderlyingType() ?: return false

    return TypeUtils.isNullableType(underlyingType)
}

fun CallableDescriptor.isGetterOfUnderlyingPropertyOfInlineClass() =
    this is PropertyGetterDescriptor && correspondingProperty.isUnderlyingPropertyOfInlineClass()

fun VariableDescriptor.isUnderlyingPropertyOfInlineClass(): Boolean =
    extensionReceiverParameter == null &&
            (containingDeclaration as? ClassDescriptor)?.inlineClassRepresentation?.underlyingPropertyName == this.name
