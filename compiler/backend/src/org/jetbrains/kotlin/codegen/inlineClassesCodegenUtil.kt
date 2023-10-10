/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.inlineClassRepresentation
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.Method

fun KotlinType.isInlineClassWithUnderlyingTypeAnyOrAnyN(): Boolean {
    val classDescriptor = constructor.declarationDescriptor
    return classDescriptor is ClassDescriptor && classDescriptor.inlineClassRepresentation?.underlyingType?.isAnyOrNullableAny() == true
}

fun CallableDescriptor.isGenericParameter(): Boolean {
    if (this !is ValueParameterDescriptor) return false
    if (containingDeclaration is AnonymousFunctionDescriptor) return true
    val index = containingDeclaration.valueParameters.indexOf(this)
    return containingDeclaration.overriddenDescriptors.any { it.original.valueParameters[index].type.isTypeParameter() }
}

fun classFileContainsMethod(descriptor: FunctionDescriptor, state: GenerationState, method: Method): Boolean? {
    if (descriptor !is DeserializedSimpleFunctionDescriptor) return null

    if (descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        return descriptor.overriddenDescriptors.any { classFileContainsMethod(it, state, method) == true }
    }

    val classId: ClassId = when {
        descriptor.containingDeclaration is DeserializedClassDescriptor -> {
            (descriptor.containingDeclaration as DeserializedClassDescriptor).classId ?: return null
        }
        descriptor.containerSource is JvmPackagePartSource -> {
            @Suppress("USELESS_CAST") // K2 warning suppression, TODO: KT-62472
            (descriptor.containerSource as JvmPackagePartSource).classId
        }
        else -> {
            return null
        }
    }

    return classFileContainsMethod(classId, state, method)
}

fun classFileContainsMethod(classId: ClassId, state: GenerationState, method: Method): Boolean? {
    val bytes = VirtualFileFinder.getInstance(state.project, state.module).findVirtualFileWithHeader(classId)
        ?.contentsToByteArray() ?: return null
    var found = false
    ClassReader(bytes).accept(object : ClassVisitor(Opcodes.API_VERSION) {
        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor? {
            if (name == method.name && descriptor == method.descriptor) {
                found = true
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }, ClassReader.SKIP_FRAMES)
    return found
}
