/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import kotlin.reflect.KClass

/**
 * Used by FirImplicitNothingAsTypeParameterCallChecker to determine when
 * resolution expected a type of Nothing, based on assignment or other
 * constraints.
 */
class ExpectedTypeAttribute(
    override val coneType: ConeKotlinType,
) : ConeAttributeWithConeType<ExpectedTypeAttribute>() {
    override fun union(other: ExpectedTypeAttribute?): ExpectedTypeAttribute? = null
    override fun intersect(other: ExpectedTypeAttribute?): ExpectedTypeAttribute? = null
    override fun add(other: ExpectedTypeAttribute?): ExpectedTypeAttribute = other ?: this
    override fun isSubtypeOf(other: ExpectedTypeAttribute?): Boolean = true
    override fun toString(): String = "Expected{${coneType.renderForDebugging()}}"
    override fun copyWith(newType: ConeKotlinType): ExpectedTypeAttribute = ExpectedTypeAttribute(newType)

    override val key: KClass<out ExpectedTypeAttribute>
        get() = ExpectedTypeAttribute::class
    override val keepInInferredDeclarationType: Boolean
        get() = true
}

val ConeAttributes.expectedType: ExpectedTypeAttribute? by ConeAttributes.attributeAccessor<ExpectedTypeAttribute>()

val ConeKotlinType.expectedType: ConeKotlinType?
    get() = attributes.expectedType?.coneType

fun ConeAttributes.addExpectedType(type: ConeKotlinType?): ConeAttributes {
    if (type == null) return this
    return add(ExpectedTypeAttribute(type))
}
