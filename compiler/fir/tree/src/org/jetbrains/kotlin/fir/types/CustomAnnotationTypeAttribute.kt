/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import kotlin.reflect.KClass

class CustomAnnotationTypeAttribute(
    val annotations: List<FirAnnotation>,
    val containerSymbols: List<FirBasedSymbol<*>> = emptyList(),
) : ConeAttribute<CustomAnnotationTypeAttribute>() {
    constructor(annotations: List<FirAnnotation>, containerSymbol: FirBasedSymbol<*>?) : this(
        annotations,
        listOfNotNull(containerSymbol),
    )

    override fun union(other: CustomAnnotationTypeAttribute?): CustomAnnotationTypeAttribute? = null

    override fun intersect(other: CustomAnnotationTypeAttribute?): CustomAnnotationTypeAttribute? = null

    override fun add(other: CustomAnnotationTypeAttribute?): CustomAnnotationTypeAttribute {
        if (other == null || other === this) return this
        return CustomAnnotationTypeAttribute(annotations + other.annotations, containerSymbols + other.containerSymbols)
    }

    override fun isSubtypeOf(other: CustomAnnotationTypeAttribute?): Boolean = true

    override fun toString(): String = annotations.joinToString(separator = " ") { it.render() }

    override val key: KClass<out CustomAnnotationTypeAttribute>
        get() = CustomAnnotationTypeAttribute::class
}

val ConeAttributes.custom: CustomAnnotationTypeAttribute? by ConeAttributes.attributeAccessor<CustomAnnotationTypeAttribute>()

val ConeAttributes.customAnnotations: List<FirAnnotation> get() = custom?.annotations.orEmpty()

val ConeKotlinType.customAnnotations: List<FirAnnotation> get() = attributes.customAnnotations
