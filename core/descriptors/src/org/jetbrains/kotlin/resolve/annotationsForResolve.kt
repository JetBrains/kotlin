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

package org.jetbrains.kotlin.resolve.descriptorUtil

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType

private val NO_INFER_ANNOTATION_FQ_NAME = FqName("kotlin.internal.NoInfer")
private val EXACT_ANNOTATION_FQ_NAME = FqName("kotlin.internal.Exact")
private val LOW_PRIORITY_IN_OVERLOAD_RESOLUTION_FQ_NAME = FqName("kotlin.internal.LowPriorityInOverloadResolution")
private val HIDES_MEMBERS_ANNOTATION_FQ_NAME = FqName("kotlin.internal.HidesMembers")
private val ONLY_INPUT_TYPES_FQ_NAME = FqName("kotlin.internal.OnlyInputTypes")
private val DYNAMIC_EXTENSION_FQ_NAME = FqName("kotlin.internal.DynamicExtension")
private val RESTRICTS_SUSPENSION_FQ_NAME = DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME.child(Name.identifier("RestrictsSuspension"))

// @HidesMembers annotation only has effect for members with these names
val HIDES_MEMBERS_NAME_LIST = setOf(Name.identifier("forEach"))

fun KotlinType.hasNoInferAnnotation(): Boolean = annotations.hasAnnotation(NO_INFER_ANNOTATION_FQ_NAME)

fun KotlinType.hasExactAnnotation(): Boolean = annotations.hasAnnotation(EXACT_ANNOTATION_FQ_NAME)

fun Annotations.hasInternalAnnotationForResolve(): Boolean =
        hasAnnotation(NO_INFER_ANNOTATION_FQ_NAME) || hasAnnotation(EXACT_ANNOTATION_FQ_NAME)

fun FqName.isInternalAnnotationForResolve() = this == NO_INFER_ANNOTATION_FQ_NAME || this == EXACT_ANNOTATION_FQ_NAME

fun CallableDescriptor.hasLowPriorityInOverloadResolution(): Boolean = annotations.hasAnnotation(LOW_PRIORITY_IN_OVERLOAD_RESOLUTION_FQ_NAME)

fun CallableDescriptor.hasHidesMembersAnnotation(): Boolean = annotations.hasAnnotation(HIDES_MEMBERS_ANNOTATION_FQ_NAME)
fun CallableDescriptor.hasDynamicExtensionAnnotation(): Boolean = annotations.hasAnnotation(DYNAMIC_EXTENSION_FQ_NAME)
fun ClassifierDescriptor.hasRestrictsSuspensionAnnotation(): Boolean = annotations.hasAnnotation(RESTRICTS_SUSPENSION_FQ_NAME)

fun TypeParameterDescriptor.hasOnlyInputTypesAnnotation(): Boolean = annotations.hasAnnotation(ONLY_INPUT_TYPES_FQ_NAME)

fun getExactInAnnotations(): Annotations = AnnotationsWithOnly(EXACT_ANNOTATION_FQ_NAME)

private class AnnotationsWithOnly(val presentAnnotation: FqName): Annotations {
    override fun iterator(): Iterator<AnnotationDescriptor> = emptyList<AnnotationDescriptor>().iterator()

    override fun isEmpty(): Boolean = false

    override fun hasAnnotation(fqName: FqName): Boolean = fqName == this.presentAnnotation

    override fun getUseSiteTargetedAnnotations(): List<AnnotationWithTarget> = emptyList()

    override fun getAllAnnotations(): List<AnnotationWithTarget> = emptyList()
}
