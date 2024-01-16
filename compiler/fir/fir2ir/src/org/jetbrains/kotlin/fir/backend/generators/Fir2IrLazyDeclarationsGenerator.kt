/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.lazy.*
import org.jetbrains.kotlin.fir.unwrapUseSiteSubstitutionOverrides
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*

class Fir2IrLazyDeclarationsGenerator(val components: Fir2IrComponents) : Fir2IrComponents by components {
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
                fir, firContainingClass, symbol, lazyParent, isFakeOverride
            ).apply {
                prepareTypeParameters()
            }
        }
        return irFunction
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
                components, startOffset, endOffset, originForProperty, fir, firContainingClass, symbols, lazyParent, isFakeOverride
            )
        }
    }

    fun createIrLazyConstructor(
        fir: FirConstructor,
        symbol: IrConstructorSymbol,
        declarationOrigin: IrDeclarationOrigin,
        lazyParent: IrDeclarationParent,
    ): Fir2IrLazyConstructor = fir.convertWithOffsets { startOffset, endOffset ->
        Fir2IrLazyConstructor(components, startOffset, endOffset, declarationOrigin, fir, symbol, lazyParent)
    }

    fun createIrLazyClass(
        firClass: FirRegularClass,
        irParent: IrDeclarationParent,
        symbol: IrClassSymbol
    ): Fir2IrLazyClass = firClass.convertWithOffsets { startOffset, endOffset ->
        val firClassOrigin = firClass.irOrigin()
        Fir2IrLazyClass(components, startOffset, endOffset, firClassOrigin, firClass, symbol, irParent)
    }

    fun createIrLazyTypeAlias(
        firTypeAlias: FirTypeAlias,
        irParent: IrDeclarationParent,
        symbol: IrTypeAliasSymbol
    ): Fir2IrLazyTypeAlias = firTypeAlias.convertWithOffsets { startOffset, endOffset ->
        Fir2IrLazyTypeAlias(
            components, startOffset, endOffset, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB, firTypeAlias, symbol, irParent
        )
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

internal fun FirCallableDeclaration.isFakeOverride(firContainingClass: FirRegularClass?): Boolean {
    val declaration = unwrapUseSiteSubstitutionOverrides()
    return declaration.isSubstitutionOrIntersectionOverride ||
            firContainingClass?.symbol?.toLookupTag() != declaration.containingClassLookupTag() ||
            declaration.isHiddenToOvercomeSignatureClash == true
}
