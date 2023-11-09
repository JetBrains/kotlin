/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.convertWithOffsets
import org.jetbrains.kotlin.fir.backend.isStubPropertyForPureField
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.lazy.*
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertyPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.utils.addToStdlib.runIf

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is dangerous API without well-defined semantics. Please, use it only if you are sure you have no other options, and be ready to breaking changes"
)
annotation class DelicateLazyGeneratorApi

class Fir2IrLazyDeclarationsGenerator(val components: Fir2IrComponents) : Fir2IrComponents by components {
    private val functionSymbolMapping = mutableMapOf<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>()
    private val propertySymbolMapping = mutableMapOf<IrPropertySymbol, IrPropertySymbol>()
    internal fun mapPropertySymbol(propertySymbol: IrPropertySymbol) = propertySymbolMapping[propertySymbol] ?: propertySymbol
    internal fun mapFunctionSymbol(functionSymbol: IrSimpleFunctionSymbol) = functionSymbolMapping[functionSymbol] ?: functionSymbol
    internal var symbolMappingEpoch: Int = 0
        private set

    /**
     * Sometimes, stages after Fir2Ir are required to do some symbol remapping.
     *
     * If it happens, there are several problems with lazy declarations
     * 1. It's hard to update lazy entries as they are not in the IR tree
     * 2. It's hard to avoid triggering a load of lazy declaration content while doing remapping
     *
     * This mechanism exists to solve the problem.
     * All lazy declarations would lazily apply this mapping to symbols inside them.
     *
     * Implementation limitation:
     *    For now, the only supported type of "symbol inside them" is overriddenSymbols inside
     *    function/property. This can be improved later if needed.
     */
    @DelicateLazyGeneratorApi
    fun registerSymbolMapping(map: Map<IrSymbol, IrSymbol>) {
        symbolMappingEpoch++
        for ((k, v) in map) {
            when {
                k is IrSimpleFunctionSymbol -> functionSymbolMapping[k] = v as IrSimpleFunctionSymbol
                k is IrPropertySymbol -> propertySymbolMapping[k] = v as IrPropertySymbol
            }
        }
    }

    internal fun createIrLazyFunction(
        fir: FirSimpleFunction,
        symbol: IrSimpleFunctionSymbol,
        lazyParent: IrDeclarationParent,
        declarationOrigin: IrDeclarationOrigin
    ): IrSimpleFunction {
        val irFunction = fir.convertWithOffsets { startOffset, endOffset ->
            val firContainingClass = (lazyParent as? Fir2IrLazyClass)?.fir
            val isFakeOverride = fir.isFakeOverride(firContainingClass)
            Fir2IrLazySimpleFunction(
                components, startOffset, endOffset, declarationOrigin,
                fir, firContainingClass, symbol, isFakeOverride
            ).apply {
                this.parent = lazyParent
                prepareTypeParameters()
            }
        }
        return irFunction
    }

    private fun FirCallableDeclaration.isFakeOverride(firContainingClass: FirRegularClass?): Boolean {
        val declaration = unwrapUseSiteSubstitutionOverrides()
        return declaration.isSubstitutionOrIntersectionOverride || firContainingClass?.symbol?.toLookupTag() != declaration.containingClassLookupTag()
    }

    internal fun createIrLazyProperty(
        fir: FirProperty,
        lazyParent: IrDeclarationParent,
        symbols: PropertySymbols,
        declarationOrigin: IrDeclarationOrigin
    ): IrProperty {
        val isPropertyForField = fir.isStubPropertyForPureField == true
        val firContainingClass = (lazyParent as? Fir2IrLazyClass)?.fir
        val isFakeOverride = !isPropertyForField && fir.isFakeOverride(firContainingClass)
        // It is really required to create those properties with DEFINED origin
        // Using `declarationOrigin` here (IR_EXTERNAL_JAVA_DECLARATION_STUB in particular) causes some tests to fail, including
        // FirPsiBlackBoxCodegenTestGenerated.Reflection.Properties.testJavaStaticField
        val originForProperty = if (isPropertyForField) IrDeclarationOrigin.DEFINED else declarationOrigin
        return fir.convertWithOffsets { startOffset, endOffset ->
            Fir2IrLazyProperty(
                components, startOffset, endOffset, originForProperty, fir, firContainingClass, symbols, isFakeOverride
            ).apply {
                this.parent = lazyParent
            }
        }
    }

    fun createIrLazyConstructor(
        fir: FirConstructor,
        symbol: IrConstructorSymbol,
        declarationOrigin: IrDeclarationOrigin,
        lazyParent: IrDeclarationParent,
    ): IrConstructor = fir.convertWithOffsets { startOffset, endOffset ->
        Fir2IrLazyConstructor(components, startOffset, endOffset, declarationOrigin, fir, symbol).apply {
            parent = lazyParent
        }
    }

    fun createIrLazyClass(
        firClass: FirRegularClass,
        irParent: IrDeclarationParent,
        symbol: IrClassSymbol
    ): Fir2IrLazyClass = firClass.convertWithOffsets { startOffset, endOffset ->
        val firClassOrigin = firClass.irOrigin(session.firProvider)
        Fir2IrLazyClass(components, startOffset, endOffset, firClassOrigin, firClass, symbol).apply {
            parent = irParent
        }
    }

    fun createIrLazyField(
        fir: FirField,
        symbol: IrFieldSymbol,
        lazyParent: IrDeclarationParent,
        declarationOrigin: IrDeclarationOrigin
    ): IrField {
        return fir.convertWithOffsets { startOffset, endOffset ->
            Fir2IrLazyField(
                components, startOffset, endOffset, declarationOrigin, fir, (lazyParent as? Fir2IrLazyClass)?.fir, symbol
            )
        }
    }
}
