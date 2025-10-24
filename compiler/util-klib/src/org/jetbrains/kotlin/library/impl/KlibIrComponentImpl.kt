/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.KlibLayoutReader
import org.jetbrains.kotlin.library.components.KlibIrComponent
import org.jetbrains.kotlin.library.components.KlibIrComponentLayout

/**
 * The default implementation of [KlibIrComponent].
 *
 * TODO (KT-81411): This class is an implementation detail. It should be made internal after dropping `KonanLibraryImpl`.
 */
class KlibIrComponentImpl(
    private val layoutReader: KlibLayoutReader<KlibIrComponentLayout>
) : KlibIrComponent {

    private val irFiles: IrArrayReader by lazy {
        IrArrayReader(layoutReader, KlibIrComponentLayout::irFilesFile)
    }

    private val irFileEntries: IrMultiArrayReader? by lazy {
        if (layoutReader.readInPlace { it.irFileEntriesFile.exists })
            IrMultiArrayReader(layoutReader, KlibIrComponentLayout::irFileEntriesFile)
        else
            null
    }

    private val combinedDeclarations: DeclarationIdMultiTableReader by lazy {
        DeclarationIdMultiTableReader(layoutReader, KlibIrComponentLayout::declarationsFile)
    }

    private val bodies: IrMultiArrayReader by lazy {
        IrMultiArrayReader(layoutReader, KlibIrComponentLayout::bodiesFile)
    }

    private val types: IrMultiArrayReader by lazy {
        IrMultiArrayReader(layoutReader, KlibIrComponentLayout::typesFile)
    }

    private val signatures: IrMultiArrayReader by lazy {
        IrMultiArrayReader(layoutReader, KlibIrComponentLayout::signaturesFile)
    }

    private val signatureDebugInfos: IrMultiArrayReader? by lazy {
        if (layoutReader.readInPlace { it.signaturesDebugInfoFile.exists })
            IrMultiArrayReader(layoutReader, KlibIrComponentLayout::signaturesDebugInfoFile)
        else
            null
    }

    private val stringLiterals: IrMultiArrayReader by lazy {
        IrMultiArrayReader(layoutReader, KlibIrComponentLayout::stringLiteralsFile)
    }

    override val irFileCount get() = irFiles.entryCount()

    override fun irFile(index: Int) = irFiles.tableItemBytes(index)
    override fun irFileEntry(index: Int, fileIndex: Int) = irFileEntries?.tableItemBytes(fileIndex, index)
    override fun declaration(index: Int, fileIndex: Int) = combinedDeclarations.tableItemBytes(fileIndex, DeclarationId(index))
    override fun body(index: Int, fileIndex: Int) = bodies.tableItemBytes(fileIndex, index)
    override fun type(index: Int, fileIndex: Int) = types.tableItemBytes(fileIndex, index)
    override fun signature(index: Int, fileIndex: Int) = signatures.tableItemBytes(fileIndex, index)
    override fun signatureDebugInfo(index: Int, fileIndex: Int) = signatureDebugInfos?.tableItemBytes(fileIndex, index)
    override fun stringLiteral(index: Int, fileIndex: Int) = stringLiterals.tableItemBytes(fileIndex, index)

    override fun irFileEntries(fileIndex: Int) = irFileEntries?.tableItemBytes(fileIndex)
    override fun declarations(fileIndex: Int) = combinedDeclarations.tableItemBytes(fileIndex)
    override fun bodies(fileIndex: Int) = bodies.tableItemBytes(fileIndex)
    override fun types(fileIndex: Int) = types.tableItemBytes(fileIndex)
    override fun signatures(fileIndex: Int) = signatures.tableItemBytes(fileIndex)
    override fun stringLiterals(fileIndex: Int) = stringLiterals.tableItemBytes(fileIndex)
}
