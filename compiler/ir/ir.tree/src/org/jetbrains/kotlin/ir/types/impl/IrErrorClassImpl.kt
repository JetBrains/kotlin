/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.LineAndColumn
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

private val ErrorFile = IrFileImpl(
    fileEntry = object : IrFileEntry {
        override val name: String = "<error-class>"
        override val maxOffset: Int
            get() = shouldNotBeCalled()

        override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo = shouldNotBeCalled()
        override fun getLineNumber(offset: Int): Int = shouldNotBeCalled()
        override fun getColumnNumber(offset: Int): Int = shouldNotBeCalled()
        override fun getLineAndColumnNumbers(offset: Int): LineAndColumn = shouldNotBeCalled()
    },
    symbol = IrFileSymbolImpl(),
    packageFqName = FqName("<error-package>")
)

val IrErrorClassImpl: IrClass = IrFactoryImpl.createClass(
    startOffset = UNDEFINED_OFFSET,
    endOffset = UNDEFINED_OFFSET,
    origin = IrDeclarationOrigin.ERROR_CLASS,
    symbol = IrClassSymbolImpl(),
    name = Name.special("<error>"),
    kind = ClassKind.CLASS,
    visibility = DescriptorVisibilities.DEFAULT_VISIBILITY,
    modality = Modality.FINAL,
    source = SourceElement.NO_SOURCE,
).apply {
    parent = ErrorFile
}
