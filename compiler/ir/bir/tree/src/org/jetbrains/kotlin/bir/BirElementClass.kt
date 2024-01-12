/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

abstract class BirElementClass<T : BirElement>(
    val javaClass: Class<T>,
    val id: Int,
    val hasImplementation: Boolean,
) : BirElementType<T> {
    override fun toString(): String = javaClass.name
}