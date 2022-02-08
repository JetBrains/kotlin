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

interface IdSignatureComputer {
    fun computeSignature(declaration: IrDeclaration): IdSignature?

    fun inFile(file: IrFileSymbol?, block: () -> Unit)
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

    override fun inFile(file: IrFileSymbol?, block: () -> Unit) {
        block()
    }
}