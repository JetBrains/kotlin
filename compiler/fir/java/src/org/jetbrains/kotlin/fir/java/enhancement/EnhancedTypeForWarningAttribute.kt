/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.fir.types.ConeAttributeWithConeType
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.renderForDebugging
import kotlin.reflect.KClass

class EnhancedTypeForWarningAttribute(
    override val coneType: ConeKotlinType,
) : ConeAttributeWithConeType<EnhancedTypeForWarningAttribute>() {
    override fun union(other: EnhancedTypeForWarningAttribute?): EnhancedTypeForWarningAttribute? = null
    override fun intersect(other: EnhancedTypeForWarningAttribute?): EnhancedTypeForWarningAttribute? = null
    override fun add(other: EnhancedTypeForWarningAttribute?): EnhancedTypeForWarningAttribute = other ?: this
    override fun isSubtypeOf(other: EnhancedTypeForWarningAttribute?): Boolean = true
    override fun toString(): String = "Enhanced for warning(${coneType.renderForDebugging()})"
    override fun copyWith(newType: ConeKotlinType): EnhancedTypeForWarningAttribute = EnhancedTypeForWarningAttribute(newType)

    override val key: KClass<out EnhancedTypeForWarningAttribute>
        get() = EnhancedTypeForWarningAttribute::class

    override val keepInInferredDeclarationType: Boolean
        get() = true
}

val ConeAttributes.enhancedTypeForWarning: EnhancedTypeForWarningAttribute? by ConeAttributes.attributeAccessor<EnhancedTypeForWarningAttribute>()

val ConeKotlinType.enhancedTypeForWarning: ConeKotlinType?
    get() = attributes.enhancedTypeForWarning?.coneType