/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.convertWithOffsets
import org.jetbrains.kotlin.fir.backend.isStubPropertyForPureField
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.dispatchReceiverClassLookupTagOrNull
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyProperty
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazySimpleFunction
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.symbols.Fir2IrPropertySymbol
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrSymbolInternals
import org.jetbrains.kotlin.ir.util.IdSignature

class Fir2IrLazyDeclarationsGenerator(val components: Fir2IrComponents) : Fir2IrComponents by components {
    internal fun createIrLazyFunction(
        fir: FirSimpleFunction,
        signature: IdSignature,
        lazyParent: IrDeclarationParent,
        declarationOrigin: IrDeclarationOrigin
    ): IrSimpleFunction {
        val symbol = symbolTable.referenceSimpleFunction(signature)
        val irFunction = fir.convertWithOffsets { startOffset, endOffset ->
            symbolTable.declareSimpleFunction(signature, { symbol }) {
                val isFakeOverride = fir.isSubstitutionOrIntersectionOverride
                Fir2IrLazySimpleFunction(
                    components, startOffset, endOffset, declarationOrigin,
                    fir, (lazyParent as? Fir2IrLazyClass)?.fir, symbol, isFakeOverride
                ).apply {
                    this.parent = lazyParent
                }
            }
        }
        functionCache[fir] = irFunction
        // NB: this is needed to prevent recursions in case of self bounds
        (irFunction as Fir2IrLazySimpleFunction).prepareTypeParameters()
        return irFunction
    }

    @OptIn(IrSymbolInternals::class)
    internal fun createIrLazyProperty(
        fir: FirProperty,
        signature: IdSignature,
        lazyParent: IrDeclarationParent,
        declarationOrigin: IrDeclarationOrigin
    ): IrProperty {
        val symbol = Fir2IrPropertySymbol(signature)
        val firPropertySymbol = fir.symbol

        fun create(startOffset: Int, endOffset: Int): Fir2IrLazyProperty {
            val isFakeOverride =
                fir.isSubstitutionOrIntersectionOverride &&
                        firPropertySymbol.dispatchReceiverClassLookupTagOrNull() !=
                        firPropertySymbol.originalForSubstitutionOverride?.dispatchReceiverClassLookupTagOrNull()
            return Fir2IrLazyProperty(
                components, startOffset, endOffset, declarationOrigin,
                fir, (lazyParent as? Fir2IrLazyClass)?.fir, symbol, isFakeOverride
            ).apply {
                this.parent = lazyParent
            }
        }

        val irProperty = fir.convertWithOffsets { startOffset, endOffset ->
            if (fir.isStubPropertyForPureField == true) {
                // Very special case when two similar properties can exist so conflicts in SymbolTable are possible.
                // See javaCloseFieldAndKotlinProperty.kt in BB tests
                symbolTable.declarePropertyWithSignature(signature, symbol)
                create(startOffset, endOffset)
                symbol.owner
            } else {
                symbolTable.declareProperty(signature, { symbol }) {
                    create(startOffset, endOffset)
                }
            }
        }
        propertyCache[fir] = irProperty
        irProperty.getter?.symbol?.let { getterForPropertyCache[symbol] = it }
        irProperty.setter?.symbol?.let { setterForPropertyCache[symbol] = it }
        irProperty.backingField?.symbol?.let {
            backingFieldForPropertyCache[symbol] = it
            propertyForBackingFieldCache[it] = symbol
        }
        return irProperty
    }

}
