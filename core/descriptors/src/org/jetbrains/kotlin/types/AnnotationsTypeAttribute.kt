/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.composeAnnotations
import kotlin.reflect.KClass

class AnnotationsTypeAttribute(val annotations: Annotations) : TypeAttribute<AnnotationsTypeAttribute>() {
    override fun union(other: AnnotationsTypeAttribute?): AnnotationsTypeAttribute? =
        if (other == this) this else null

    override fun intersect(other: AnnotationsTypeAttribute?): AnnotationsTypeAttribute? =
        if (other == this) this else null

    override fun add(other: AnnotationsTypeAttribute?): AnnotationsTypeAttribute {
        if (other == null) return this
        return AnnotationsTypeAttribute(composeAnnotations(annotations, other.annotations))
    }

    override fun isSubtypeOf(other: AnnotationsTypeAttribute?): Boolean = true

    override val key: KClass<out AnnotationsTypeAttribute>
        get() = AnnotationsTypeAttribute::class

    override fun equals(other: Any?): Boolean {
        if (other !is AnnotationsTypeAttribute) return false
        return other.annotations == this.annotations
    }

    override fun hashCode(): Int = annotations.hashCode()
}

val TypeAttributes.annotationsAttribute: AnnotationsTypeAttribute? by TypeAttributes.attributeAccessor<AnnotationsTypeAttribute>()

val TypeAttributes.annotations: Annotations get() = annotationsAttribute?.annotations ?: Annotations.EMPTY
