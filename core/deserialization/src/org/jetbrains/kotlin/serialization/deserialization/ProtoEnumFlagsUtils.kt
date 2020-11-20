/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.metadata.ProtoBuf

@Suppress("unused")
fun ProtoEnumFlags.memberKind(memberKind: ProtoBuf.MemberKind?): CallableMemberDescriptor.Kind = when (memberKind) {
    ProtoBuf.MemberKind.DECLARATION -> CallableMemberDescriptor.Kind.DECLARATION
    ProtoBuf.MemberKind.FAKE_OVERRIDE -> CallableMemberDescriptor.Kind.FAKE_OVERRIDE
    ProtoBuf.MemberKind.DELEGATION -> CallableMemberDescriptor.Kind.DELEGATION
    ProtoBuf.MemberKind.SYNTHESIZED -> CallableMemberDescriptor.Kind.SYNTHESIZED
    else -> CallableMemberDescriptor.Kind.DECLARATION
}

@Suppress("unused")
fun ProtoEnumFlags.memberKind(kind: CallableMemberDescriptor.Kind): ProtoBuf.MemberKind = when (kind) {
    CallableMemberDescriptor.Kind.DECLARATION -> ProtoBuf.MemberKind.DECLARATION
    CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> ProtoBuf.MemberKind.FAKE_OVERRIDE
    CallableMemberDescriptor.Kind.DELEGATION -> ProtoBuf.MemberKind.DELEGATION
    CallableMemberDescriptor.Kind.SYNTHESIZED -> ProtoBuf.MemberKind.SYNTHESIZED
}

@Suppress("unused")
fun ProtoEnumFlags.descriptorVisibility(visibility: ProtoBuf.Visibility?): DescriptorVisibility = when (visibility) {
    ProtoBuf.Visibility.INTERNAL -> DescriptorVisibilities.INTERNAL
    ProtoBuf.Visibility.PRIVATE -> DescriptorVisibilities.PRIVATE
    ProtoBuf.Visibility.PRIVATE_TO_THIS -> DescriptorVisibilities.PRIVATE_TO_THIS
    ProtoBuf.Visibility.PROTECTED -> DescriptorVisibilities.PROTECTED
    ProtoBuf.Visibility.PUBLIC -> DescriptorVisibilities.PUBLIC
    ProtoBuf.Visibility.LOCAL -> DescriptorVisibilities.LOCAL
    else -> DescriptorVisibilities.PRIVATE
}

@Suppress("unused")
fun ProtoEnumFlags.descriptorVisibility(visibility: DescriptorVisibility): ProtoBuf.Visibility = when (visibility) {
    DescriptorVisibilities.INTERNAL -> ProtoBuf.Visibility.INTERNAL
    DescriptorVisibilities.PUBLIC -> ProtoBuf.Visibility.PUBLIC
    DescriptorVisibilities.PRIVATE -> ProtoBuf.Visibility.PRIVATE
    DescriptorVisibilities.PRIVATE_TO_THIS -> ProtoBuf.Visibility.PRIVATE_TO_THIS
    DescriptorVisibilities.PROTECTED -> ProtoBuf.Visibility.PROTECTED
    DescriptorVisibilities.LOCAL -> ProtoBuf.Visibility.LOCAL
    else -> throw IllegalArgumentException("Unknown visibility: $visibility")
}
