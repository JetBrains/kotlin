/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

const val UNDEFINED_OFFSET: Int = -1

data class SourceRangeInfo(
    val filePath: String,
    val startOffset: Int,
    val startLineNumber: Int,
    val startColumnNumber: Int,
    val endOffset: Int,
    val endLineNumber: Int,
    val endColumnNumber: Int
)

interface IrFileEntry {
    val name: String
    val maxOffset: Int
    fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo
    fun getLineNumber(offset: Int): Int
    fun getColumnNumber(offset: Int): Int
}
