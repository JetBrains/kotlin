/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterPublicSymbolImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IdSignatureComposer
import org.jetbrains.kotlin.ir.util.NameProvider
import org.jetbrains.kotlin.ir.util.SymbolTable

class IcSymbolTable(
    signaturer: IdSignatureComposer,
    irFactory: IrFactory,
    nameProvider: NameProvider = NameProvider.DEFAULT,
) : SymbolTable(
    signaturer,
    irFactory,
    nameProvider,
) {
    override fun referenceFieldFromLinker(sig: IdSignature): IrFieldSymbol =
        fieldSymbolTable.run {
            fieldSymbolTable.referenced(sig) { IrFieldPublicSymbolImpl(sig) }
        }

    override fun declareGlobalTypeParameter(
        sig: IdSignature,
        symbolFactory: () -> IrTypeParameterSymbol,
        typeParameterFactory: (IrTypeParameterSymbol) -> IrTypeParameter
    ): IrTypeParameter {
        return globalTypeParameterSymbolTable.declare(sig, symbolFactory, typeParameterFactory)
    }

    override fun referenceTypeParameterFromLinker(sig: IdSignature): IrTypeParameterSymbol {
        return scopedTypeParameterSymbolTable.get(sig) ?: globalTypeParameterSymbolTable.referenced(sig) {
            IrTypeParameterPublicSymbolImpl(sig)
        }
    }
}