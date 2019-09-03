/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.inline

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils

val INLINE_ONLY_ANNOTATION_FQ_NAME = FqName("kotlin.internal.InlineOnly")

/**
 * @return true if it's impossible to observe a call instruction referencing this member in the bytecode.
 */
fun MemberDescriptor.isEffectivelyInlineOnly(): Boolean =
    isInlineWithReified() || isInlineOnlyPrivateInBytecode()

/**
 * @return true if this member should be private in bytecode because it's effectively inline-only.
 */
fun MemberDescriptor.isInlineOnlyPrivateInBytecode(): Boolean =
    isInlineOnly() || isPrivateInlineSuspend()

fun MemberDescriptor.isInlineOnly(): Boolean =
    this is FunctionDescriptor && isInline &&
            (hasInlineOnlyAnnotation() || DescriptorUtils.getDirectMember(this).hasInlineOnlyAnnotation())

private fun MemberDescriptor.isPrivateInlineSuspend(): Boolean =
    this is FunctionDescriptor && isSuspend && isInline && visibility == Visibilities.PRIVATE

fun MemberDescriptor.isInlineWithReified(): Boolean =
    this is CallableMemberDescriptor && (hasReifiedParameters() || DescriptorUtils.getDirectMember(this).hasReifiedParameters())

private fun CallableMemberDescriptor.hasReifiedParameters(): Boolean =
    typeParameters.any { it.isReified }

private fun CallableMemberDescriptor.hasInlineOnlyAnnotation(): Boolean =
    annotations.hasAnnotation(INLINE_ONLY_ANNOTATION_FQ_NAME)
