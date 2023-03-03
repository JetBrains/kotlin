/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.name.FqName

@PrettyIrDsl
class IrFileBuilder @PublishedApi internal constructor(private val name: String, buildingContext: IrBuildingContext) :
    IrPackageFragmentBuilder<IrFile>(buildingContext),
    IrAnnotationContainerBuilder,
    IrDeclarationContainerBuilder,
    IrSymbolOwnerBuilder {

    override var builtAnnotations: List<IrConstructorCall> by SetAtMostOnce(emptyList())

    override val declarationBuilders = mutableListOf<IrDeclarationBuilder<*>>()

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

    @PublishedApi
    override fun build(): IrFile {
        return IrFileImpl(
            fileEntry = SyntheticFileEntry(name),
            symbol = symbol<IrFileSymbol>(::IrFileSymbolImpl),
            fqName = packageFqName,
        ).also {
            recordSymbolFromOwner(it)
            addDeclarationsTo(it)
            addAnnotationsTo(it)
        }
    }
}
