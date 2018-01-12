/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.ir.declarations.IrFile

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

interface SourceManager {
    interface FileEntry {
        val name: String
        val maxOffset: Int
        fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo
        fun getLineNumber(offset: Int): Int
        fun getColumnNumber(offset: Int): Int
    }

    fun getFileEntry(irFile: IrFile): FileEntry?
}
