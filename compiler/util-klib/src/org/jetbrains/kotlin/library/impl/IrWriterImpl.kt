/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.*

abstract class IrWriterImpl(val irLayout: IrKotlinLibraryLayout) : IrWriter {
    init {
        irLayout.irDir.mkdirs()
    }

    override fun addDataFlowGraph(dataFlowGraph: ByteArray) {
        irLayout.dataFlowGraphFile.writeBytes(dataFlowGraph)
    }
}

class IrMonoliticWriterImpl(irLayout: IrKotlinLibraryLayout) : IrWriterImpl(irLayout) {

    override fun addIr(ir: SerializedIrModule) {
        with(ir.files.sortedBy { it.path }) {
            IrArrayWriter(map { it.fileData }).writeIntoFile(irLayout.irFiles.absolutePath)
            IrArrayWriter(map { IrMemoryDeclarationWriter(it.declarations).writeIntoMemory() }).writeIntoFile(irLayout.irDeclarations.absolutePath)
            IrArrayWriter(map { IrMemoryArrayWriter(it.symbols).writeIntoMemory() }).writeIntoFile(irLayout.irSymbols.absolutePath)
            IrArrayWriter(map { IrMemoryArrayWriter(it.types).writeIntoMemory() }).writeIntoFile(irLayout.irTypes.absolutePath)
            IrArrayWriter(map { IrMemoryArrayWriter(it.strings).writeIntoMemory() }).writeIntoFile(irLayout.irStrings.absolutePath)
            IrArrayWriter(map { IrMemoryArrayWriter(it.bodies).writeIntoMemory() }).writeIntoFile(irLayout.irBodies.absolutePath)
        }
    }
}

class IrPerFileWriterImpl(irLayout: IrKotlinLibraryLayout) : IrWriterImpl(irLayout) {
    override fun addIr(ir: SerializedIrModule) {
        ir.files.forEach {
            serializeFile(it)
        }
    }

    private fun serializeFile(file: SerializedIrFile) {
        val fqnPath = file.fqName.joinToString(separator = ".")
        val fileId = file.path.hashCode().toString(Character.MAX_RADIX)
        val irFileDirectory = "$fqnPath.$fileId.file"
        val fileDir = irLayout.irDir.child(irFileDirectory)

        assert(!fileDir.exists)
        fileDir.mkdirs()

        irLayout.irFile(fileDir).writeBytes(file.fileData)
        IrDeclarationWriter(file.declarations).writeIntoFile(irLayout.irDeclarations(fileDir).absolutePath)
        IrArrayWriter(file.symbols).writeIntoFile(irLayout.irSymbols(fileDir).absolutePath)
        IrArrayWriter(file.types).writeIntoFile(irLayout.irTypes(fileDir).absolutePath)
        IrArrayWriter(file.strings).writeIntoFile(irLayout.irStrings(fileDir).absolutePath)
        IrArrayWriter(file.bodies).writeIntoFile(irLayout.irBodies(fileDir).absolutePath)
//        fileDir.child("declarations.txt").writeText(file.declarations.joinToString(separator = "\n") { "${it.declarationName} -> (${it.id}, ${it.local})" })
    }
}