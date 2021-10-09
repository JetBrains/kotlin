/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import kotlin.reflect.KClass

class CustomAnnotationTypeAttribute(val annotations: Annotations) : TypeAttribute<CustomAnnotationTypeAttribute>() {
    constructor(annotations: List<AnnotationDescriptor>) : this(Annotations.create(annotations))

    override fun union(other: CustomAnnotationTypeAttribute?): CustomAnnotationTypeAttribute? = null

    override fun intersect(other: CustomAnnotationTypeAttribute?): CustomAnnotationTypeAttribute? = null

    override fun add(other: CustomAnnotationTypeAttribute?): CustomAnnotationTypeAttribute {
        if (other == null) return this
        return CustomAnnotationTypeAttribute(Annotations.create(annotations + other.annotations))
    }

    override fun isSubtypeOf(other: CustomAnnotationTypeAttribute?): Boolean = true

    override val key: KClass<out CustomAnnotationTypeAttribute>
        get() = CustomAnnotationTypeAttribute::class
}

val TypeAttributes.custom: CustomAnnotationTypeAttribute? by TypeAttributes.attributeAccessor<CustomAnnotationTypeAttribute>()

val TypeAttributes.customAnnotations: Annotations get() = custom?.annotations ?: Annotations.EMPTY
