/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.IrKotlinLibraryLayout
import org.jetbrains.kotlin.library.IrWriter
import org.jetbrains.kotlin.library.SerializedIrModule

class IrWriterImpl(val irLayout: IrKotlinLibraryLayout) : IrWriter {
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
            val fileEntries = map { it.fileEntries }
            if (fileEntries.any { it.isNotEmpty() }) {
                IrArrayWriter(fileEntries).writeIntoFile(irLayout.irFileEntries.absolutePath)
            }
        }
    }
}
