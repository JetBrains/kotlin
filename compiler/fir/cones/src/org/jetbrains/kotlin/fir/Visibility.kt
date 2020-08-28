/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

abstract class Visibility protected constructor(
    val name: String,
    val isPublicAPI: Boolean
) {
    open val internalDisplayName: String
        get() = name

    final override fun toString() = internalDisplayName

    open fun compareTo(visibility: Visibility): Int? {
        return Visibilities.compareLocal(this, visibility)
    }

    open fun normalize(): Visibility = this
}

