/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET

@JvmInline
value class SourceSpan constructor(
    private val packedValue: Long
) {
    constructor(start: Int, end: Int) : this(start.toUInt().toLong() or (end.toLong() shl 32))

    val start: Int
        get() = packedValue.toInt()
    val end: Int
        get() = (packedValue shr 32).toInt()

    val isUndefined: Boolean
        get() = this == UNDEFINED

    override fun toString() = "[$start, $end]"

    companion object {
        val UNDEFINED = SourceSpan(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        val SYNTHETIC = SourceSpan(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
    }
}