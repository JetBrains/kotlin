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

    private fun serializeFile(file: IrIrSerializedIrFile) {
        val fqnPath = file.fqName.joinToString(separator = "/")
        val fileId = file.path.hashCode().toString(Character.MAX_RADIX)

        val fileDirectoryName = "${fileId}.file"

        val packageDir = irLayout.irDir.child(fqnPath)
        if (!packageDir.exists) {
            packageDir.mkdirs()
        }

        val fileDir = packageDir.child(fileDirectoryName)
        assert(!fileDir.exists)
        fileDir.mkdirs()


        irLayout.irFile(fileDir).writeBytes(file.fileData)
        IrDeclarationWriter(file.declarations).writeIntoFile(irLayout.irDeclarations(fileDir).absolutePath)
        IrArrayWriter(file.symbols).writeIntoFile(irLayout.irSymbols(fileDir).absolutePath)
        IrArrayWriter(file.types).writeIntoFile(irLayout.irTypes(fileDir).absolutePath)
        IrArrayWriter(file.strings).writeIntoFile(irLayout.irStrings(fileDir).absolutePath)
        IrArrayWriter(file.bodies).writeIntoFile(irLayout.irBodies(fileDir).absolutePath)
        fileDir.child("declarations.txt").writeText(file.declarations.joinToString(separator = "\n") { "${it.declarationName} -> (${it.id}, ${it.local})" })
    }

    override fun addIr(ir: SerializedIrModule) {
//        ir.files.forEach {
//            serializeFile(it)
//        }

        with(ir.files.sortedBy { it.path }) {
            IrArrayWriter(map { it.fileData }).writeIntoFile(irLayout.irFiles.absolutePath)
            IrArrayWriter(map { IrMemoryDeclarationWriter(it.declarations).writeIntoMemory() }).writeIntoFile(irLayout.irDeclarations.absolutePath)
            IrArrayWriter(map { IrMemoryArrayWriter(it.symbols).writeIntoMemory() }).writeIntoFile(irLayout.irSymbols.absolutePath)
            IrArrayWriter(map { IrMemoryArrayWriter(it.types).writeIntoMemory() }).writeIntoFile(irLayout.irTypes.absolutePath)
            IrArrayWriter(map { IrMemoryArrayWriter(it.strings).writeIntoMemory() }).writeIntoFile(irLayout.irStrings.absolutePath)
            IrArrayWriter(map { IrMemoryArrayWriter(it.bodies).writeIntoMemory() }).writeIntoFile(irLayout.irBodies.absolutePath)
        }


//        IrDeclarationWriter(ir.serializedDeclarations).writeIntoFile(irLayout.irDeclarations.absolutePath)
//        IrArrayWriter(ir.symbols).writeIntoFile(irLayout.irSymbols.absolutePath)
//        IrArrayWriter(ir.types).writeIntoFile(irLayout.irTypes.absolutePath)
//        IrArrayWriter(ir.strings).writeIntoFile(irLayout.irStrings.absolutePath)
    }

    override fun addDataFlowGraph(dataFlowGraph: ByteArray) {
        irLayout.dataFlowGraphFile.writeBytes(dataFlowGraph)
    }
}
