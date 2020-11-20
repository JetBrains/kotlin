/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

abstract class Visibility protected constructor(
    val name: String,
    val isPublicAPI: Boolean
) {
    open val internalDisplayName: String
        get() = name

    open val externalDisplayName: String
        get() = internalDisplayName

    abstract fun mustCheckInImports(): Boolean

    open fun compareTo(visibility: Visibility): Int? {
        return Visibilities.compareLocal(this, visibility)
    }

    final override fun toString() = internalDisplayName

    open fun normalize(): Visibility = this

    // Should be overloaded in Java visibilities
    open fun customEffectiveVisibility(): EffectiveVisibility? = null
}
