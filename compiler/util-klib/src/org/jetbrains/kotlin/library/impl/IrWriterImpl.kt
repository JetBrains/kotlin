/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.IrKotlinLibraryLayout
import org.jetbrains.kotlin.library.IrWriter
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.SerializedIrModule

abstract class IrWriterImpl(val irLayout: IrKotlinLibraryLayout) : IrWriter {
    override fun addDataFlowGraph(dataFlowGraph: ByteArray) {
        irLayout.dataFlowGraphFile.writeBytes(dataFlowGraph)
    }
}

class IrMonoliticWriterImpl(_irLayout: IrKotlinLibraryLayout) : IrWriterImpl(_irLayout) {

    override fun addIr(ir: SerializedIrModule) {
        irLayout.irDir.mkdirs()

        with(ir.files.sortedBy { it.path }) {
            IrArrayWriter(map { it.fileData }).writeIntoFile(irLayout.irFiles.absolutePath)
            IrArrayWriter(map { it.declarations }).writeIntoFile(irLayout.irDeclarations.absolutePath)
            IrArrayWriter(map { it.types }).writeIntoFile(irLayout.irTypes.absolutePath)
            IrArrayWriter(map { it.signatures }).writeIntoFile(irLayout.irSignatures.absolutePath)
            IrArrayWriter(map { it.strings }).writeIntoFile(irLayout.irStrings.absolutePath)
            IrArrayWriter(map { it.bodies }).writeIntoFile(irLayout.irBodies.absolutePath)
            IrArrayWriter(mapNotNull { it.debugInfo }).writeIntoFile(irLayout.irDebugInfo.absolutePath)
        }
    }
}

class IrPerFileWriterImpl(_irLayout: IrKotlinLibraryLayout) : IrWriterImpl(_irLayout) {
    override fun addIr(ir: SerializedIrModule) {
        irLayout.irDir.mkdirs()

        ir.files.forEach {
            serializeFile(it)
        }
    }

    private fun serializeFile(file: SerializedIrFile) {
        val fqnPath = file.fqName
        val fileId = file.path.hashCode().toString(Character.MAX_RADIX)
        val irFileDirectory = "$fqnPath.$fileId.file"
        val fileDir = irLayout.irDir.child(irFileDirectory)

        assert(!fileDir.exists)
        fileDir.mkdirs()

        irLayout.irFile(fileDir).writeBytes(file.fileData)

        irLayout.irDeclarations(fileDir).writeBytes(file.declarations)
        irLayout.irTypes(fileDir).writeBytes(file.types)
        irLayout.irSignatures(fileDir).writeBytes(file.signatures)
        irLayout.irStrings(fileDir).writeBytes(file.strings)
        irLayout.irBodies(fileDir).writeBytes(file.bodies)
    }
}