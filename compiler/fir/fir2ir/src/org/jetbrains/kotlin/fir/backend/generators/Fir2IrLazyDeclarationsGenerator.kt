/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.convertWithOffsets
import org.jetbrains.kotlin.fir.backend.irOrigin
import org.jetbrains.kotlin.fir.backend.isStubPropertyForPureField
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyConstructor
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyProperty
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazySimpleFunction
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertyPublicSymbolImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.utils.addToStdlib.runIf

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
                val firContainingClass = (lazyParent as? Fir2IrLazyClass)?.fir
                val isFakeOverride = fir.isFakeOverride(firContainingClass)
                Fir2IrLazySimpleFunction(
                    components, startOffset, endOffset, declarationOrigin,
                    fir, firContainingClass, symbol, isFakeOverride
                ).apply {
                    this.parent = lazyParent
                }
            }
        }
        // NB: this is needed to prevent recursions in case of self bounds
        (irFunction as Fir2IrLazySimpleFunction).prepareTypeParameters()
        return irFunction
    }

    private fun FirCallableDeclaration.isFakeOverride(firContainingClass: FirRegularClass?): Boolean {
        val declaration = unwrapUseSiteSubstitutionOverrides()
        return declaration.isSubstitutionOrIntersectionOverride || firContainingClass?.symbol?.toLookupTag() != declaration.containingClassLookupTag()
    }

    internal fun createIrLazyProperty(
        fir: FirProperty,
        signature: IdSignature,
        lazyParent: IrDeclarationParent,
        declarationOrigin: IrDeclarationOrigin
    ): IrProperty {
        val symbol = IrPropertyPublicSymbolImpl(signature)
        fun create(startOffset: Int, endOffset: Int, isPropertyForField: Boolean): Fir2IrLazyProperty {
            val firContainingClass = (lazyParent as? Fir2IrLazyClass)?.fir
            val isFakeOverride = !isPropertyForField && fir.isFakeOverride(firContainingClass)
            return Fir2IrLazyProperty(
                components, startOffset, endOffset, declarationOrigin,
                fir, firContainingClass, symbol, isFakeOverride
            ).apply {
                this.parent = lazyParent
            }
        }

        val irProperty = fir.convertWithOffsets { startOffset, endOffset ->
            if (fir.isStubPropertyForPureField == true) {
                // Very special case when two similar properties can exist so conflicts in SymbolTable are possible.
                // See javaCloseFieldAndKotlinProperty.kt in BB tests
                symbolTable.declarePropertyWithSignature(signature, symbol)
                create(startOffset, endOffset, isPropertyForField = true)
            } else {
                symbolTable.declareProperty(signature, { symbol }) {
                    create(startOffset, endOffset, isPropertyForField = false)
                }
            }
        }
        return irProperty
    }

    fun createIrLazyConstructor(
        fir: FirConstructor,
        signature: IdSignature,
        declarationOrigin: IrDeclarationOrigin,
        lazyParent: IrDeclarationParent,
    ): IrConstructor = fir.convertWithOffsets { startOffset, endOffset ->
        symbolTable.declareConstructor(signature, { IrConstructorPublicSymbolImpl(signature) }) { symbol ->
            Fir2IrLazyConstructor(components, startOffset, endOffset, declarationOrigin, fir, symbol).apply {
                parent = lazyParent
            }
        }
    }

    fun createIrLazyClass(
        firClass: FirRegularClass,
        irParent: IrDeclarationParent,
    ): IrClass = firClass.convertWithOffsets { startOffset, endOffset ->
        val firClassOrigin = firClass.irOrigin(session.firProvider)
        val signature = runIf(configuration.linkViaSignatures) {
            signatureComposer.composeSignature(firClass)
        }
        classifiersGenerator.declareIrClass(signature) { symbol ->
            Fir2IrLazyClass(components, startOffset, endOffset, firClassOrigin, firClass, symbol).apply {
                parent = irParent
            }
        }
    }
}
