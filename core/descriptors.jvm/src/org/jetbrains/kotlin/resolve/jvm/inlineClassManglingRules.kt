/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound

fun shouldHideConstructorDueToInlineClassTypeValueParameters(descriptor: CallableMemberDescriptor): Boolean {
    val constructorDescriptor = descriptor as? ClassConstructorDescriptor ?: return false
    if (DescriptorVisibilities.isPrivate(constructorDescriptor.visibility)) return false
    if (constructorDescriptor.constructedClass.isInlineClass()) return false
    if (DescriptorUtils.isSealedClass(constructorDescriptor.constructedClass)) return false

    // TODO inner class in inline class

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
    return returnType.isInlineClassType()
}

fun DeclarationDescriptor.isInlineClassThatRequiresMangling(): Boolean =
    isInlineClass() && !isDontMangleClass(this as ClassDescriptor)

fun KotlinType.isInlineClassThatRequiresMangling() =
    constructor.declarationDescriptor?.isInlineClassThatRequiresMangling() == true

private fun KotlinType.requiresFunctionNameManglingInParameterTypes() =
    isInlineClassThatRequiresMangling() || isTypeParameterWithUpperBoundThatRequiresMangling()

private fun isDontMangleClass(classDescriptor: ClassDescriptor) =
    classDescriptor.fqNameSafe == StandardNames.RESULT_FQ_NAME

private fun KotlinType.isTypeParameterWithUpperBoundThatRequiresMangling(): Boolean {
    val descriptor = constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
    return descriptor.representativeUpperBound.requiresFunctionNameManglingInParameterTypes()
}
