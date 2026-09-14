/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.impl

import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.components.KlibIrComponent

fun klibIrComponentFromFiles(files: List<SerializedIrFile>): KlibIrComponent {
    return KlibIrComponentInMemoryImpl(files)
}

internal class KlibIrComponentInMemoryImpl(
    files: List<SerializedIrFile>
) : AbstractKlibIrComponentImpl() {
    override val irFiles: IrArrayReader by lazy {
        IrArrayReader(files.map { it.fileData })
    }

    override val irFileEntries: IrMultiArrayReader? by lazy {
        if (files.any { it.fileEntries == null }) return@lazy null
        IrMultiArrayReader(files.mapNotNull { it.fileEntries })
    }

    override val combinedDeclarations: DeclarationIdMultiTableReader by lazy {
        DeclarationIdMultiTableReader(files.map { it.declarations })
    }

    override val bodies: IrMultiArrayReader by lazy {
        IrMultiArrayReader(files.map { it.bodies })
    }

    override val types: IrMultiArrayReader by lazy {
        IrMultiArrayReader(files.map { it.types })
    }

    override val signatures: IrMultiArrayReader by lazy {
        IrMultiArrayReader(files.map { it.signatures })
    }

    override val signatureDebugInfos: IrMultiArrayReader? by lazy {
        if (files.any { it.debugInfo == null }) return@lazy null
        IrMultiArrayReader(files.mapNotNull { it.debugInfo })
    }

    override val stringLiterals: IrMultiArrayReader by lazy {
        IrMultiArrayReader(files.map { it.strings })
    }
}
