/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import kotlin.reflect.KClass

class UnsafeDowncastWrtVarianceAttribute(
    override val coneType: ConeKotlinType,
) : ConeAttributeWithConeType<UnsafeDowncastWrtVarianceAttribute>() {
    override fun union(other: UnsafeDowncastWrtVarianceAttribute?): UnsafeDowncastWrtVarianceAttribute? = null
    override fun intersect(other: UnsafeDowncastWrtVarianceAttribute?): UnsafeDowncastWrtVarianceAttribute? = null
    override fun add(other: UnsafeDowncastWrtVarianceAttribute?): UnsafeDowncastWrtVarianceAttribute = other ?: this
    override fun isSubtypeOf(other: UnsafeDowncastWrtVarianceAttribute?): Boolean = true
    override fun toString(): String = "(future ${coneType.renderForDebugging()})"
    override fun copyWith(newType: ConeKotlinType): UnsafeDowncastWrtVarianceAttribute = UnsafeDowncastWrtVarianceAttribute(newType)

    override val key: KClass<out UnsafeDowncastWrtVarianceAttribute>
        get() = UnsafeDowncastWrtVarianceAttribute::class
    override val keepInInferredDeclarationType: Boolean
        get() = true
}

val ConeAttributes.unsafeDowncastWrtVariance: UnsafeDowncastWrtVarianceAttribute? by ConeAttributes.attributeAccessor<UnsafeDowncastWrtVarianceAttribute>()
