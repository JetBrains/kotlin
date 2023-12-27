/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase

abstract class BirElementClass(
    val javaClass: Class<*>,
    val id: Int,
    val hasImplementation: Boolean,
) {
    override fun toString(): String = javaClass.name
}
