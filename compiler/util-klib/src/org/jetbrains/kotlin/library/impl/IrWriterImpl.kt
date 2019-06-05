/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.*

class IrWriterImpl(val irLayout: IrKotlinLibraryLayout) : IrWriter {
    init {
        irLayout.irDir.mkdirs()
        irLayout.irTablesDir.mkdirs()
    }

    override fun addIr(ir: SerializedIr) {
        irLayout.irHeader.writeBytes(ir.module)
        // TODO: use Files.move.
        File(ir.combinedDeclarationFilePath).copyTo(irLayout.irDeclarations)
        File(ir.symbolTableFilePath).copyTo(irLayout.irSymbols)
        File(ir.typeTableFilePath).copyTo(irLayout.irTypes)
        File(ir.stringTableFilePath).copyTo(irLayout.irStrings)
    }

    override fun addDataFlowGraph(dataFlowGraph: ByteArray) {
        irLayout.dataFlowGraphFile.writeBytes(dataFlowGraph)
    }
}
