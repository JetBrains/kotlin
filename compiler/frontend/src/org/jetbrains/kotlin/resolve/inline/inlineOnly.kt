/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.inline

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils

private val INLINE_ONLY_ANNOTATION_FQ_NAME = FqName("kotlin.internal.InlineOnly")

fun MemberDescriptor.isInlineOnlyOrReifiable(): Boolean =
    this is CallableMemberDescriptor && (isReifiable() || DescriptorUtils.getDirectMember(this).isReifiable() || isInlineOnly())

fun MemberDescriptor.isEffectivelyInlineOnly(): Boolean =
    isInlineOnlyOrReifiable() || (this is FunctionDescriptor && isSuspend && isInline &&
            (valueParameters.any { it.isCrossinline } || visibility == Visibilities.PRIVATE))

fun MemberDescriptor.isInlineOnly(): Boolean =
    this is FunctionDescriptor && isInline &&
            (hasInlineOnlyAnnotation() || DescriptorUtils.getDirectMember(this).hasInlineOnlyAnnotation())

private fun CallableMemberDescriptor.isReifiable() = typeParameters.any { it.isReified }

private fun CallableMemberDescriptor.hasInlineOnlyAnnotation() = annotations.hasAnnotation(INLINE_ONLY_ANNOTATION_FQ_NAME)
