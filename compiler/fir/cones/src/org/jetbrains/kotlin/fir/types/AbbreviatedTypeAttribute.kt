/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import kotlin.reflect.KClass

class AbbreviatedTypeAttribute(
    val coneType: ConeKotlinType
): ConeAttribute<AbbreviatedTypeAttribute>() {
    override fun union(other: AbbreviatedTypeAttribute?): AbbreviatedTypeAttribute? = null
    override fun intersect(other: AbbreviatedTypeAttribute?): AbbreviatedTypeAttribute? = null
    override fun add(other: AbbreviatedTypeAttribute?): AbbreviatedTypeAttribute? = null
    override fun isSubtypeOf(other: AbbreviatedTypeAttribute?): Boolean = true
    override fun toString(): String = "{${coneType.renderForDebugging()}=}"

    override val key: KClass<out AbbreviatedTypeAttribute>
        get() = AbbreviatedTypeAttribute::class
}

val ConeAttributes.abbreviatedType: AbbreviatedTypeAttribute? by ConeAttributes.attributeAccessor<AbbreviatedTypeAttribute>()

val ConeKotlinType.abbreviatedType: ConeKotlinType?
    get() = attributes.abbreviatedType?.coneType