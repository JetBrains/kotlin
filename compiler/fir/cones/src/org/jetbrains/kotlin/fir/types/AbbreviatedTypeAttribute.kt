/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import kotlin.reflect.KClass

class AbbreviatedTypeAttribute(
    override val coneType: ConeKotlinType,
) : ConeAttributeWithConeType<AbbreviatedTypeAttribute>() {
    override fun union(other: AbbreviatedTypeAttribute?): AbbreviatedTypeAttribute? = null
    override fun intersect(other: AbbreviatedTypeAttribute?): AbbreviatedTypeAttribute? = null
    override fun add(other: AbbreviatedTypeAttribute?): AbbreviatedTypeAttribute = other ?: this
    override fun isSubtypeOf(other: AbbreviatedTypeAttribute?): Boolean = true
    override fun toString(): String = "{${coneType.renderForDebugging()}=}"
    override fun copyWith(newType: ConeKotlinType): AbbreviatedTypeAttribute = AbbreviatedTypeAttribute(newType)

    override val key: KClass<out AbbreviatedTypeAttribute>
        get() = AbbreviatedTypeAttribute::class
    override val keepInInferredDeclarationType: Boolean
        get() = true
}

val ConeAttributes.abbreviatedType: AbbreviatedTypeAttribute? by ConeAttributes.attributeAccessor<AbbreviatedTypeAttribute>()

val ConeKotlinType.abbreviatedType: ConeKotlinType?
    get() = attributes.abbreviatedType?.coneType