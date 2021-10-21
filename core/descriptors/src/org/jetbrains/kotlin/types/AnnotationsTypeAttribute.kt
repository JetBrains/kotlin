/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import kotlin.reflect.KClass

class AnnotationsTypeAttribute(val annotations: Annotations) : TypeAttribute<AnnotationsTypeAttribute>() {
    constructor(annotations: List<AnnotationDescriptor>) : this(Annotations.create(annotations))

    override fun union(other: AnnotationsTypeAttribute?): AnnotationsTypeAttribute? = null

    override fun intersect(other: AnnotationsTypeAttribute?): AnnotationsTypeAttribute? = null

    override fun add(other: AnnotationsTypeAttribute?): AnnotationsTypeAttribute {
        if (other == null) return this
        return AnnotationsTypeAttribute(Annotations.create(annotations + other.annotations))
    }

    override fun isSubtypeOf(other: AnnotationsTypeAttribute?): Boolean = true

    override val key: KClass<out AnnotationsTypeAttribute>
        get() = AnnotationsTypeAttribute::class

    override fun equals(other: Any?): Boolean {
        if (other !is AnnotationsTypeAttribute) return false
        return annotations == other.annotations
    }
}

val TypeAttributes.annotationsAttribute: AnnotationsTypeAttribute? by TypeAttributes.attributeAccessor<AnnotationsTypeAttribute>()

val TypeAttributes.annotations: Annotations get() = annotationsAttribute?.annotations ?: Annotations.EMPTY
