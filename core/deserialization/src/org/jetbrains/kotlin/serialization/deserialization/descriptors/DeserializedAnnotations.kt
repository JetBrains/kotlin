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

package org.jetbrains.kotlin.serialization.deserialization.descriptors

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.toReadOnlyList

class DeserializedAnnotations(
        storageManager: StorageManager,
        compute: () -> List<AnnotationDescriptor>
) : DeserializedAnnotationsWithPossibleTargets(
        storageManager,
        { compute().map { AnnotationWithTarget(it, null) } })

open class DeserializedAnnotationsWithPossibleTargets(
        storageManager: StorageManager,
        compute: () -> List<AnnotationWithTarget>
) : Annotations {
    private val annotations = storageManager.createLazyValue { compute().toReadOnlyList() }

    override fun isEmpty(): Boolean = annotations().isEmpty()

    override fun findAnnotation(fqName: FqName) = annotations().firstOrNull {
        annotationWithTarget ->
        if (annotationWithTarget.target != null) return@firstOrNull false
        val descriptor = annotationWithTarget.annotation.type.constructor.declarationDescriptor
        descriptor is ClassDescriptor && fqName.equalsTo(DescriptorUtils.getFqName(descriptor))
    }?.annotation

    override fun findExternalAnnotation(fqName: FqName) = null

    override fun getUseSiteTargetedAnnotations() = annotations().filter { it.target != null }

    override fun getAllAnnotations() = annotations()

    override fun iterator(): Iterator<AnnotationDescriptor> {
        return annotations().asSequence().filter { it.target == null }.map { it.annotation }.iterator()
    }
}