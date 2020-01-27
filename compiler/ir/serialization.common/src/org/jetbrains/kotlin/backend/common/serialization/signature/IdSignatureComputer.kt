/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.signature

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.util.IdSignature

interface IdSignatureComputer {
    fun computeSignature(declaration: IrDeclaration): IdSignature?
}

class DescToIrIdSignatureComputer(private val delegate: IdSignatureDescriptor) : IdSignatureComputer {
    override fun computeSignature(declaration: IrDeclaration): IdSignature? {
        return if (declaration is IrEnumEntry) delegate.composeEnumEntrySignature(declaration.descriptor)
        else delegate.composeSignature(declaration.descriptor)
    }
}