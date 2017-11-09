/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

class AnnotationsImpl : Annotations {
    private val annotations: List<AnnotationDescriptor>
    private val targetedAnnotations: List<AnnotationWithTarget>

    constructor(annotations: List<AnnotationDescriptor>) {
        this.annotations = annotations
        this.targetedAnnotations = annotations.map { AnnotationWithTarget(it, null) }
    }

    // List<AnnotationDescriptor> and List<AnnotationWithTarget> have the same signature
    private constructor(
            targetedAnnotations: List<AnnotationWithTarget>,
            @Suppress("UNUSED_PARAMETER") i: Int
    ) {
        this.targetedAnnotations = targetedAnnotations
        this.annotations = targetedAnnotations.filter { it.target == null }.map { it.annotation }
    }

    override fun isEmpty() = targetedAnnotations.isEmpty()

    override fun getUseSiteTargetedAnnotations(): List<AnnotationWithTarget> {
        return targetedAnnotations
                .filter { it.target != null }
                .map { AnnotationWithTarget(it.annotation, it.target!!) }
    }

    override fun getAllAnnotations() = targetedAnnotations

    override fun iterator() = annotations.iterator()

    override fun toString() = annotations.toString()

    companion object {
        @JvmStatic
        fun create(annotationsWithTargets: List<AnnotationWithTarget>): AnnotationsImpl {
            return AnnotationsImpl(annotationsWithTargets, 0)
        }
    }
}
