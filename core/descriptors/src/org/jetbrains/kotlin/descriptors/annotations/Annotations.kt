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

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils

public interface Annotations : Iterable<AnnotationDescriptor> {

    public fun isEmpty(): Boolean

    public fun findAnnotation(fqName: FqName): AnnotationDescriptor?

    public fun findExternalAnnotation(fqName: FqName): AnnotationDescriptor?

    public fun getUseSiteTargetedAnnotations(): List<AnnotationWithTarget>

    public fun getUseSiteTargetedAnnotations(target: AnnotationUseSiteTarget): List<AnnotationDescriptor> {
        return getUseSiteTargetedAnnotations().fold(arrayListOf<AnnotationDescriptor>()) { list, targeted ->
            if (target == targeted.target) {
                list.add(targeted.annotation)
            }
            list
        }
    }

    // Returns both targeted and annotations without target. Annotation order is preserved.
    public fun getAllAnnotations(): List<AnnotationWithTarget>

    companion object {
        public val EMPTY: Annotations = object : Annotations {
            override fun isEmpty() = true

            override fun findAnnotation(fqName: FqName) = null

            override fun findExternalAnnotation(fqName: FqName) = null

            override fun getUseSiteTargetedAnnotations() = emptyList<AnnotationWithTarget>()

            override fun getAllAnnotations() = emptyList<AnnotationWithTarget>()

            override fun iterator() = emptyList<AnnotationDescriptor>().iterator()

            override fun toString() = "EMPTY"
        }
    }
}

class FilteredAnnotations(
        private val delegate: Annotations,
        private val fqNameFilter: (FqName) -> Boolean
) : Annotations {
    override fun findAnnotation(fqName: FqName) =
            if (fqNameFilter(fqName)) delegate.findAnnotation(fqName)
            else null

    override fun findExternalAnnotation(fqName: FqName) =
            if (fqNameFilter(fqName)) delegate.findExternalAnnotation(fqName)
            else null

    override fun getUseSiteTargetedAnnotations(): List<AnnotationWithTarget> {
        return delegate.getUseSiteTargetedAnnotations().filter { shouldBeReturned(it.annotation) }
    }

    override fun getAllAnnotations(): List<AnnotationWithTarget> {
        return delegate.getAllAnnotations().filter { shouldBeReturned(it.annotation) }
    }

    override fun iterator() = delegate.filter { shouldBeReturned(it) }.iterator()

    private fun shouldBeReturned(annotation: AnnotationDescriptor): Boolean {
        val descriptor = annotation.getType().getConstructor().getDeclarationDescriptor()
        return descriptor != null && DescriptorUtils.getFqName(descriptor).let { fqName ->
            fqName.isSafe() && fqNameFilter(fqName.toSafe())
        }
    }

    override fun isEmpty() = !iterator().hasNext()
}

class CompositeAnnotations(
        private val delegates: List<Annotations>
) : Annotations {
    constructor(vararg delegates: Annotations): this(delegates.toList())

    override fun isEmpty() = delegates.all { it.isEmpty() }

    override fun findAnnotation(fqName: FqName) = delegates.asSequence().map { it.findAnnotation(fqName) }.filterNotNull().firstOrNull()

    override fun findExternalAnnotation(fqName: FqName) = delegates.asSequence().map { it.findExternalAnnotation(fqName) }.filterNotNull().firstOrNull()

    override fun getUseSiteTargetedAnnotations() = delegates.flatMap { it.getUseSiteTargetedAnnotations() }

    override fun getAllAnnotations() = delegates.flatMap { it.getAllAnnotations() }

    override fun iterator() = delegates.asSequence().flatMap { it.asSequence() }.iterator()
}