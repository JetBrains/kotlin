/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.signature

import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.util.IdSignature

/**
 * Something capable of computing an [IdSignature] for an [IrDeclaration].
 */
interface IdSignatureComputer {

    /**
     * Computes a signature of [declaration].
     *
     * @param declaration The declaration to compute the signature for.
     * @return The signature of the [declaration], or `null` if the declaration cannot have a signature (for example,
     * because it is not exportable according to [org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker]).
     */
    fun computeSignature(declaration: IrDeclaration): IdSignature?

    /**
     * Informs the signature computer that all signature computations for top-level private declarations within [block] will use
     * the [file]'s signature (a signature for a top-level private declaration should always contain a signature of the file this
     * declaration is declared in).
     *
     * @param file A symbol of the file for declarations in which signatures will be computed in [block], or `null` if the declarations
     * won't have a file associated (like some compiler generated declarations).
     * @param block A block within which signatures computed for private declarations will include [file]'s signature.
     * @see [IdSignature.FileSignature]
     */
    fun <R> inFile(file: IrFileSymbol?, block: () -> R): R
}

class DescToIrIdSignatureComputer(private val delegate: IdSignatureDescriptor) : IdSignatureComputer {
    override fun computeSignature(declaration: IrDeclaration): IdSignature? {
        return when (declaration) {
            is IrEnumEntry -> delegate.composeEnumEntrySignature(declaration.descriptor)
            is IrField -> delegate.composeFieldSignature(declaration.descriptor)
            is IrAnonymousInitializer -> delegate.composeAnonInitSignature(declaration.descriptor)
            else -> delegate.composeSignature(declaration.descriptor)
        }
    }

    override fun <R> inFile(file: IrFileSymbol?, block: () -> R): R = block()
}
