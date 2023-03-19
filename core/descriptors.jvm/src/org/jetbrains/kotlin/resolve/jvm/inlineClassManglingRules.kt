/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound

fun shouldHideConstructorDueToValueClassTypeValueParameters(descriptor: CallableMemberDescriptor): Boolean {
    val constructorDescriptor = descriptor as? ClassConstructorDescriptor ?: return false
    if (DescriptorVisibilities.isPrivate(constructorDescriptor.visibility)) return false
    if (constructorDescriptor.constructedClass.isValueClass()) return false
    if (DescriptorUtils.isSealedClass(constructorDescriptor.constructedClass)) return false

    return constructorDescriptor.valueParameters.any { it.type.requiresFunctionNameManglingInParameterTypes() }
}

fun requiresFunctionNameManglingForParameterTypes(descriptor: CallableMemberDescriptor): Boolean {
    val extensionReceiverType = descriptor.extensionReceiverParameter?.type
    return extensionReceiverType != null && extensionReceiverType.requiresFunctionNameManglingInParameterTypes() ||
            descriptor.valueParameters.any { it.type.requiresFunctionNameManglingInParameterTypes() }
}

// NB functions returning all inline classes (including our special 'kotlin.Result') should be mangled.
fun requiresFunctionNameManglingForReturnType(descriptor: CallableMemberDescriptor): Boolean {
    if (descriptor.containingDeclaration !is ClassDescriptor) return false
    val returnType = descriptor.returnType ?: return false
    return returnType.isInlineClassType() || returnType.isTypeParameterWithUpperBoundThatRequiresMangling(includeMfvc = false)
}

fun DeclarationDescriptor.isValueClassThatRequiresMangling(): Boolean =
    isValueClass() && !isDontMangleClass(this as ClassDescriptor)

fun KotlinType.isValueClassThatRequiresMangling() =
    constructor.declarationDescriptor?.let { it.isInlineClass() && it.isValueClassThatRequiresMangling() || needsMfvcFlattening() } == true

private fun KotlinType.requiresFunctionNameManglingInParameterTypes() =
    isValueClassThatRequiresMangling() || isTypeParameterWithUpperBoundThatRequiresMangling(includeMfvc = true)

private fun isDontMangleClass(classDescriptor: ClassDescriptor) =
    classDescriptor.fqNameSafe == StandardNames.RESULT_FQ_NAME

private fun KotlinType.isTypeParameterWithUpperBoundThatRequiresMangling(includeMfvc: Boolean): Boolean {
    val descriptor = constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
    return (includeMfvc || !descriptor.isMultiFieldValueClass()) && descriptor.representativeUpperBound.requiresFunctionNameManglingInParameterTypes()
}
