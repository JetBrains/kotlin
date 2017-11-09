/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.descriptors.annotations

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun KotlinBuiltIns.createDeprecatedAnnotation(
        message: String,
        replaceWith: String = "",
        level: String = "WARNING"
): AnnotationDescriptor {
    val replaceWithAnnotation = BuiltInAnnotationDescriptor(
            this,
            KotlinBuiltIns.FQ_NAMES.replaceWith,
            mapOf(
                    REPLACE_WITH_EXPRESSION_NAME to StringValue(replaceWith, this),
                    REPLACE_WITH_IMPORTS_NAME to ArrayValue(emptyList(), getArrayType(Variance.INVARIANT, stringType), this)
            )
    )

    return BuiltInAnnotationDescriptor(
            this,
            KotlinBuiltIns.FQ_NAMES.deprecated,
            mapOf(
                    DEPRECATED_MESSAGE_NAME to StringValue(message, this),
                    DEPRECATED_REPLACE_WITH_NAME to AnnotationValue(replaceWithAnnotation),
                    DEPRECATED_LEVEL_NAME to EnumValue(getDeprecationLevelEnumEntry(level) ?: error("Deprecation level $level not found"))
            )
    )
}

private val DEPRECATED_MESSAGE_NAME = Name.identifier("message")
private val DEPRECATED_REPLACE_WITH_NAME = Name.identifier("replaceWith")
private val DEPRECATED_LEVEL_NAME = Name.identifier("level")
private val REPLACE_WITH_EXPRESSION_NAME = Name.identifier("expression")
private val REPLACE_WITH_IMPORTS_NAME = Name.identifier("imports")

private val INLINE_ONLY_ANNOTATION_FQ_NAME = FqName("kotlin.internal.InlineOnly")

fun MemberDescriptor.isInlineOnlyOrReifiable(): Boolean =
        this is CallableMemberDescriptor && (isReifiable() || DescriptorUtils.getDirectMember(this).isReifiable() || isInlineOnly())

fun MemberDescriptor.isEffectivelyInlineOnly(): Boolean =
        isInlineOnlyOrReifiable() || safeAs<FunctionDescriptor>()?.let { it.isSuspend && it.isInline } == true

fun MemberDescriptor.isInlineOnly(): Boolean {
    if (this !is FunctionDescriptor ||
        !(hasInlineOnlyAnnotation() || DescriptorUtils.getDirectMember(this).hasInlineOnlyAnnotation())) return false
    assert(isInline) { "Function is not inline: $this" }
    return true
}

private fun CallableMemberDescriptor.isReifiable() = typeParameters.any { it.isReified }

private fun CallableMemberDescriptor.hasInlineOnlyAnnotation() = annotations.hasAnnotation(INLINE_ONLY_ANNOTATION_FQ_NAME)
