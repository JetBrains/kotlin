/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import kotlin.reflect.KClass

/**
 * [AbbreviatedTypeAttribute] stores the abbreviated type ([coneType]) of its owning expanded type. In the compiler, it is used exclusively
 * for rendering the abbreviated typed in place of the expanded type. The Analysis API uses this attribute for additional purposes such as
 * navigation to the type alias.
 *
 * The abbreviated type may not always be resolvable from a use-site session. For example, the owning expanded type may come from a library
 * `L2` with a dependency on another library `L1` containing the type alias declaration. If the use-site session depends on `L2` but not on
 * `L1`, the abbreviated type won't be resolvable even if the expanded type is.
 */
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

val ConeKotlinType.abbreviatedTypeOrSelf: ConeKotlinType
    get() = abbreviatedType ?: this

val ConeKotlinType.isTypealiasExpansion: Boolean
    get() = this != abbreviatedTypeOrSelf
