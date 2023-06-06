/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi.impl

internal class BooleanFlag(private val index: UInt) {
    operator fun invoke(state: Boolean): Int = if (state) 1 shl index.toInt() else 0
    operator fun invoke(flags: Int): Boolean = (flags ushr index.toInt()) and 1 != 0
}
