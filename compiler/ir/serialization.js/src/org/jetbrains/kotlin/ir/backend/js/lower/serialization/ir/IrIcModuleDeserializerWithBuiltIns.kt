/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializerWithBuiltIns
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.knownBuiltins
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.descriptors.IrAbstractFunctionFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature

class IrIcModuleDeserializerWithBuiltIns(
    builtIns: IrBuiltIns,
    functionFactory: IrAbstractFunctionFactory,
    delegate: IrModuleDeserializer,
) : IrModuleDeserializerWithBuiltIns(builtIns, functionFactory, delegate) {

    override fun additionalBuiltIns(builtIns: IrBuiltIns): Map<IdSignature, IrSymbol> {
        val result = mutableMapOf<IdSignature, IrSymbol>()

        builtIns.knownBuiltins.forEach {
            val symbol = (it as IrSymbolOwner).symbol
            val declaration = symbol.owner
            if (declaration is IrSimpleFunction) {
                declaration.typeParameters.forEachIndexed { i, tp ->
                    result[IdSignature.GlobalFileLocalSignature(symbol.signature!!, 1000_000_000_000L + i, "")] = tp.symbol
                }
            }
        }

        return result
    }

    override fun checkIsFunctionInterface(idSig: IdSignature): Boolean {
        if (idSig is IdSignature.GlobalFileLocalSignature) return checkIsFunctionInterface(idSig.container)
        return super.checkIsFunctionInterface(idSig)
    }

    override fun contains(idSig: IdSignature): Boolean {
        return super.contains(idSig) || idSig is IdSignature.GlobalFileLocalSignature && checkIsFunctionInterface(idSig.container)
    }

    override fun resolveFunctionalInterface(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        if (idSig is IdSignature.GlobalFileLocalSignature) {
            val containerSymbolKind = when (idSig.container.asPublic()!!.nameSegments.size) {
                1 -> BinarySymbolData.SymbolKind.CLASS_SYMBOL
                3 -> BinarySymbolData.SymbolKind.FUNCTION_SYMBOL
                else -> error("Cannot infer symbolKind")
            }

            val declaration = resolveFunctionalInterface(idSig.container, containerSymbolKind).owner as IrTypeParametersContainer

            return declaration.typeParameters[(idSig.id - 1000_000_000_000L).toInt()].symbol
        }

        return super.resolveFunctionalInterface(idSig, symbolKind)
    }
}