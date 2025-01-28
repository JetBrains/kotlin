/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import kotlin.reflect.KClass

object DeprecatedBareDowncastAttribute : ConeAttribute<DeprecatedBareDowncastAttribute>() {
    override fun union(other: DeprecatedBareDowncastAttribute?): DeprecatedBareDowncastAttribute? = null
    override fun intersect(other: DeprecatedBareDowncastAttribute?): DeprecatedBareDowncastAttribute? = null
    override fun add(other: DeprecatedBareDowncastAttribute?): DeprecatedBareDowncastAttribute = other ?: this
    override fun isSubtypeOf(other: DeprecatedBareDowncastAttribute?): Boolean = true
    override fun toString(): String = "(deprecated bare downcast)"

    override val key: KClass<out DeprecatedBareDowncastAttribute>
        get() = DeprecatedBareDowncastAttribute::class
    override val keepInInferredDeclarationType: Boolean
        get() = true
}

val ConeAttributes.deprecatedBareDowncast: DeprecatedBareDowncastAttribute? by ConeAttributes.attributeAccessor<DeprecatedBareDowncastAttribute>()
