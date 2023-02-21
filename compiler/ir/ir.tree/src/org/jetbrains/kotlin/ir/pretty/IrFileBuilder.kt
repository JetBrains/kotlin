/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.name.FqName

@PrettyIrDsl
class IrFileBuilder internal constructor(override val symbolContext: SymbolContext, private val name: String) : IrElementBuilder<IrFile>(),
    IrAnnotationContainerBuilder,
    IrDeclarationContainerBuilder,
    IrSymbolOwnerBuilder {

    private var packageFqName: String by SetAtMostOnce("")

    override var builtAnnotations: List<IrConstructorCall> by SetAtMostOnce(emptyList())

    override val declarationBuilders = mutableListOf<IrDeclarationBuilder<*>>()

    override var symbolReference: String? by SetAtMostOnce(null)

    private class SyntheticFileEntry(override val name: String) : IrFileEntry {
        override val maxOffset: Int
            get() = UNDEFINED_OFFSET

        override val supportsDebugInfo: Boolean
            get() = false // TODO: Support debug info

        override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo {
            error("SyntheticFileEntry doesn't support debug info")
        }

        override fun getLineNumber(offset: Int): Int {
            error("SyntheticFileEntry doesn't support debug info")
        }

        override fun getColumnNumber(offset: Int): Int {
            error("SyntheticFileEntry doesn't support debug info")
        }
    }

    @Deprecated(
        "Custom debug info is not supported for IrFile",
        replaceWith = ReplaceWith(""),
        level = DeprecationLevel.ERROR,
    )
    override fun debugInfo(startOffset: Int, endOffset: Int) {
        throw UnsupportedOperationException("Custom debug info is not supported for IrFile")
    }

    @PrettyIrDsl
    fun packageName(name: String) {
        packageFqName = name
    }

    override fun build(): IrFile {
        return IrFileImpl(
            fileEntry = SyntheticFileEntry(name),
            symbol = symbol<IrFileSymbol>(::IrFileSymbolImpl),
            fqName = FqName(packageFqName),
        ).also {
            recordSymbolFromOwner(it)
            addDeclarationsTo(it)
            addAnnotationsTo(it)
        }
    }
}
