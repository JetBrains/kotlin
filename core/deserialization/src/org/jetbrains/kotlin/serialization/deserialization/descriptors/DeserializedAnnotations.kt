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

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue

open class DeserializedAnnotations(
        storageManager: StorageManager,
        compute: () -> List<AnnotationDescriptor>
) : Annotations {
    private val annotations by storageManager.createLazyValue(compute)

    override fun isEmpty(): Boolean = annotations.isEmpty()

    override fun findAnnotation(fqName: FqName) =
            annotations.firstOrNull { annotation -> annotation.annotationClass?.fqNameUnsafe == fqName.toUnsafe() }

    override fun getUseSiteTargetedAnnotations(): List<AnnotationWithTarget> = emptyList()

    override fun getAllAnnotations(): List<AnnotationWithTarget> = annotations.map { AnnotationWithTarget(it, null) }

    override fun iterator(): Iterator<AnnotationDescriptor> = annotations.iterator()
}

class NonEmptyDeserializedAnnotations(
        storageManager: StorageManager,
        compute: () -> List<AnnotationDescriptor>
) : DeserializedAnnotations(storageManager, compute) {
    override fun isEmpty(): Boolean = false
}

open class DeserializedAnnotationsWithPossibleTargets(
        storageManager: StorageManager,
        compute: () -> List<AnnotationWithTarget>
) : Annotations {
    private val annotations by storageManager.createLazyValue(compute)

    override fun isEmpty(): Boolean = annotations.isEmpty()

    override fun findAnnotation(fqName: FqName): AnnotationDescriptor? =
            annotations.firstOrNull { (annotation, target) ->
                target == null && annotation.annotationClass?.fqNameUnsafe == fqName.toUnsafe()
            }?.annotation

    override fun getUseSiteTargetedAnnotations(): List<AnnotationWithTarget> = annotations.filter { it.target != null }

    override fun getAllAnnotations(): List<AnnotationWithTarget> = annotations

    override fun iterator(): Iterator<AnnotationDescriptor> {
        return annotations.asSequence().filter { it.target == null }.map { it.annotation }.iterator()
    }
}

class NonEmptyDeserializedAnnotationsWithPossibleTargets(
        storageManager: StorageManager,
        compute: () -> List<AnnotationWithTarget>
) : DeserializedAnnotationsWithPossibleTargets(storageManager, compute) {
    override fun isEmpty(): Boolean = false
}
