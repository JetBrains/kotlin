/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

@JvmInline
value class SourceSpan constructor(
    private val packed: Long
) {
    constructor(start: Int, end: Int) : this(start.toLong() or (end.toLong() shl 32))

    val start: Int
        get() = packed.toInt()
    val end: Int
        get() = (packed shr 32).toInt()

    val isUndefined: Boolean
        get() = packed == -1L

    override fun toString() = "[$start, $end]"

    companion object {
        val UNDEFINED = SourceSpan(-1)
        val SYNTHETIC = SourceSpan(-2)
    }
}