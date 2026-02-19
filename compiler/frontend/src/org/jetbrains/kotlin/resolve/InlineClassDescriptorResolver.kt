/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name

object InlineClassDescriptorResolver {
    @JvmField
    val BOX_METHOD_NAME = Name.identifier("box")

    @JvmField
    val UNBOX_METHOD_NAME = Name.identifier("unbox")

    @JvmField
    val SPECIALIZED_EQUALS_NAME = Name.identifier("equals-impl0")

    val BOXING_VALUE_PARAMETER_NAME = Name.identifier("v")

    val SPECIALIZED_EQUALS_FIRST_PARAMETER_NAME = Name.identifier("p1")
    val SPECIALIZED_EQUALS_SECOND_PARAMETER_NAME = Name.identifier("p2")

    @JvmStatic
    fun isSynthesizedBoxMethod(descriptor: CallableMemberDescriptor) =
        isSynthesizedInlineClassMemberWithName(descriptor, BOX_METHOD_NAME)

    @JvmStatic
    fun isSynthesizedUnboxMethod(descriptor: CallableMemberDescriptor) =
        isSynthesizedInlineClassMemberWithName(descriptor, UNBOX_METHOD_NAME)

    @JvmStatic
    fun isSynthesizedBoxOrUnboxMethod(descriptor: CallableMemberDescriptor) =
        isSynthesizedInlineClassMember(descriptor) && (descriptor.name == BOX_METHOD_NAME || descriptor.name == UNBOX_METHOD_NAME)

    @JvmStatic
    fun isSpecializedEqualsMethod(descriptor: CallableMemberDescriptor) =
        isSynthesizedInlineClassMemberWithName(descriptor, SPECIALIZED_EQUALS_NAME)

    private fun isSynthesizedInlineClassMemberWithName(descriptor: CallableMemberDescriptor, name: Name) =
        isSynthesizedInlineClassMember(descriptor) && descriptor.name == name

    private fun isSynthesizedInlineClassMember(descriptor: CallableMemberDescriptor) =
        descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED && descriptor.containingDeclaration.isInlineClass()
}
