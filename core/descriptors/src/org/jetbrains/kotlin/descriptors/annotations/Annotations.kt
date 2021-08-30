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

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.model.AnnotationMarker

interface Annotated {
    val annotations: Annotations
}

interface Annotations : Iterable<AnnotationDescriptor> {
    fun isEmpty(): Boolean

    fun findAnnotation(fqName: FqName): AnnotationDescriptor? = firstOrNull { it.fqName == fqName }

    fun hasAnnotation(fqName: FqName): Boolean = findAnnotation(fqName) != null

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("This method should only be used in frontend where we split annotations according to their use-site targets.")
    fun getUseSiteTargetedAnnotations(): List<AnnotationWithTarget> = emptyList()

    companion object {
        val EMPTY: Annotations = object : Annotations {
            override fun isEmpty() = true

            override fun findAnnotation(fqName: FqName) = null

            override fun iterator() = emptyList<AnnotationDescriptor>().iterator()

            override fun toString() = "EMPTY"
        }

        fun create(annotations: List<AnnotationDescriptor>): Annotations =
            if (annotations.isEmpty()) EMPTY else AnnotationsImpl(annotations)
    }
}

class FilteredAnnotations(
    private val delegate: Annotations,
    private val isDefinitelyNewInference: Boolean,
    private val fqNameFilter: (FqName) -> Boolean
) : Annotations {

    constructor(delegate: Annotations, fqNameFilter: (FqName) -> Boolean) : this(delegate, false, fqNameFilter)

    override fun hasAnnotation(fqName: FqName) =
        if (fqNameFilter(fqName)) delegate.hasAnnotation(fqName)
        else false

    override fun findAnnotation(fqName: FqName) =
        if (fqNameFilter(fqName)) delegate.findAnnotation(fqName)
        else null

    override fun iterator() = delegate.filter(this::shouldBeReturned).iterator()

    override fun isEmpty(): Boolean {
        val condition = delegate.any(this::shouldBeReturned)
        // fixing KT-32189 && KT-32138 for the new inference only
        return if (isDefinitelyNewInference) !condition else condition
    }

    private fun shouldBeReturned(annotation: AnnotationDescriptor): Boolean =
        annotation.fqName.let { fqName ->
            fqName != null && fqNameFilter(fqName)
        }
}

class FilteredByPredicateAnnotations(
    private val delegate: Annotations,
    private val filter: (AnnotationDescriptor) -> Boolean
) : Annotations {
    override fun isEmpty(): Boolean {
        return !iterator().hasNext()
    }

    override fun iterator(): Iterator<AnnotationDescriptor> {
        return delegate.filter(filter).iterator()
    }

    override fun findAnnotation(fqName: FqName): AnnotationDescriptor? {
        return super.findAnnotation(fqName)?.takeIf(filter)
    }
}

class CompositeAnnotations(
    private val delegates: List<Annotations>
) : Annotations {
    constructor(vararg delegates: Annotations) : this(delegates.toList())

    override fun isEmpty() = delegates.all { it.isEmpty() }

    override fun hasAnnotation(fqName: FqName) = delegates.asSequence().any { it.hasAnnotation(fqName) }

    override fun findAnnotation(fqName: FqName) = delegates.asSequence().mapNotNull { it.findAnnotation(fqName) }.firstOrNull()

    @Suppress("DEPRECATION", "OverridingDeprecatedMember", "OVERRIDE_DEPRECATION")
    override fun getUseSiteTargetedAnnotations() = delegates.flatMap { it.getUseSiteTargetedAnnotations() }

    override fun iterator() = delegates.asSequence().flatMap { it.asSequence() }.iterator()
}

fun composeAnnotations(first: Annotations, second: Annotations) =
    when {
        first.isEmpty() -> second
        second.isEmpty() -> first
        else -> CompositeAnnotations(first, second)
    }
