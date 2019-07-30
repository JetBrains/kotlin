/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.*

class IrWriterImpl(val irLayout: IrKotlinLibraryLayout) : IrWriter {
    init {
        irLayout.irDir.mkdirs()
        irLayout.irTablesDir.mkdirs()
    }

    override fun addIr(ir: SerializedIr) {
        irLayout.irHeader.writeBytes(ir.module)

        IrDeclarationWriter(ir.serializedDeclarations).writeIntoFile(irLayout.irDeclarations.absolutePath)
        IrTableWriter(ir.symbols).writeIntoFile(irLayout.irSymbols.absolutePath)
        IrTableWriter(ir.types).writeIntoFile(irLayout.irTypes.absolutePath)
        IrTableWriter(ir.strings).writeIntoFile(irLayout.irStrings.absolutePath)
    }

    override fun addDataFlowGraph(dataFlowGraph: ByteArray) {
        irLayout.dataFlowGraphFile.writeBytes(dataFlowGraph)
    }
}
