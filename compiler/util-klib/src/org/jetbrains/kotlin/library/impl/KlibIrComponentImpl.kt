/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.KlibLayoutReader
import org.jetbrains.kotlin.library.components.KlibIrComponent
import org.jetbrains.kotlin.library.components.KlibIrComponentLayout
import kotlin.io.path.exists

internal abstract class AbstractKlibIrComponentImpl() : KlibIrComponent {
    protected abstract val irFiles: IrArrayReader
    protected abstract val irFileEntries: IrMultiArrayReader?
    protected abstract val combinedDeclarations: DeclarationIdMultiTableReader
    protected abstract val bodies: IrMultiArrayReader
    protected abstract val types: IrMultiArrayReader
    protected abstract val signatures: IrMultiArrayReader
    protected abstract val signatureDebugInfos: IrMultiArrayReader?
    protected abstract val stringLiterals: IrMultiArrayReader

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

/**
 * The default implementation of [KlibIrComponent].
 */
internal class KlibIrComponentImpl(
    private val layoutReader: KlibLayoutReader<KlibIrComponentLayout>
) : AbstractKlibIrComponentImpl() {

    override val irFiles: IrArrayReader by lazy {
        IrArrayReader(layoutReader, KlibIrComponentLayout::irFilesFile)
    }

    override val irFileEntries: IrMultiArrayReader? by lazy {
        if (layoutReader.readInPlace { it.irFileEntriesFile.exists() })
            IrMultiArrayReader(layoutReader, KlibIrComponentLayout::irFileEntriesFile)
        else
            null
    }

    override val combinedDeclarations: DeclarationIdMultiTableReader by lazy {
        DeclarationIdMultiTableReader(layoutReader, KlibIrComponentLayout::declarationsFile)
    }

    override val bodies: IrMultiArrayReader by lazy {
        IrMultiArrayReader(layoutReader, KlibIrComponentLayout::bodiesFile)
    }

    override val types: IrMultiArrayReader by lazy {
        IrMultiArrayReader(layoutReader, KlibIrComponentLayout::typesFile)
    }

    override val signatures: IrMultiArrayReader by lazy {
        IrMultiArrayReader(layoutReader, KlibIrComponentLayout::signaturesFile)
    }

    override val signatureDebugInfos: IrMultiArrayReader? by lazy {
        if (layoutReader.readInPlace { it.signaturesDebugInfoFile.exists() })
            IrMultiArrayReader(layoutReader, KlibIrComponentLayout::signaturesDebugInfoFile)
        else
            null
    }

    override val stringLiterals: IrMultiArrayReader by lazy {
        IrMultiArrayReader(layoutReader, KlibIrComponentLayout::stringLiteralsFile)
    }
}
