/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.LineAndColumn
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

class MultifileFacadeFileEntry(
    private val className: JvmClassName,
    val partFiles: List<IrFile>
) : IrFileEntry {
    override val name: String
        get() = "<multi-file facade $className>"

    override val maxOffset: Int
        get() = UNDEFINED_OFFSET

    override val supportsDebugInfo get() = false

    override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo =
        error("Multifile facade doesn't support debug info: $className")

    override fun getLineNumber(offset: Int): Int =
        error("Multifile facade doesn't support debug info: $className")

    override fun getColumnNumber(offset: Int): Int =
        error("Multifile facade doesn't support debug info: $className")

    override fun getLineAndColumnNumbers(offset: Int): LineAndColumn =
        error("Multifile facade doesn't support debug info: $className")
}

fun IrFunction.isMultifileBridge(): Boolean =
    (parent as? IrClass)?.origin == IrDeclarationOrigin.JVM_MULTIFILE_CLASS
