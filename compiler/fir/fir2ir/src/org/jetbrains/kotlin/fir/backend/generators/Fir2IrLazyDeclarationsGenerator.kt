/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.utils.convertWithOffsets
import org.jetbrains.kotlin.fir.backend.utils.declareThisReceiverParameter
import org.jetbrains.kotlin.fir.backend.utils.irOrigin
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.lazy.*
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyConstructor
import org.jetbrains.kotlin.fir.unwrapUseSiteSubstitutionOverrides
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.parentClassOrNull

class Fir2IrLazyDeclarationsGenerator(private val c: Fir2IrComponents) : Fir2IrComponents by c {
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
                c, startOffset, endOffset, declarationOrigin,
                fir, firContainingClass, symbol, lazyParent, isFakeOverride
            )
        }

        irFunction.prepareTypeParameters()

        declarationStorage.enterScope(symbol)

        irFunction.extensionReceiverParameter = fir.receiverParameter?.let {
            irFunction.declareThisReceiverParameter(
                c,
                thisType = it.typeRef.toIrType(typeConverter),
                thisOrigin = irFunction.origin,
                explicitReceiver = it,
            )
        }

        val containingClass = lazyParent as? IrClass
        if (containingClass != null && irFunction.shouldHaveDispatchReceiver(containingClass)) {
            val thisType = Fir2IrCallableDeclarationsGenerator.computeDispatchReceiverType(irFunction, fir, containingClass, c)
            irFunction.dispatchReceiverParameter = irFunction.declareThisReceiverParameter(
                c,
                thisType = thisType ?: error("No dispatch receiver receiver for function"),
                thisOrigin = irFunction.origin
            )
        }

        declarationStorage.leaveScope(symbol)
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
                c, startOffset, endOffset, originForProperty, fir, firContainingClass, symbols, lazyParent, isFakeOverride
            )
        }
    }

    fun createIrLazyConstructor(
        fir: FirConstructor,
        symbol: IrConstructorSymbol,
        declarationOrigin: IrDeclarationOrigin,
        lazyParent: IrDeclarationParent,
    ): Fir2IrLazyConstructor {
        val irConstructor = fir.convertWithOffsets { startOffset, endOffset ->
            Fir2IrLazyConstructor(c, startOffset, endOffset, declarationOrigin, fir, symbol, lazyParent)
        }

        declarationStorage.enterScope(symbol)

        val containingClass = lazyParent as? IrClass
        val outerClass = containingClass?.parentClassOrNull
        if (containingClass?.isInner == true && outerClass != null) {
            irConstructor.dispatchReceiverParameter = irConstructor.declareThisReceiverParameter(
                c,
                thisType = outerClass.thisReceiver!!.type,
                thisOrigin = irConstructor.origin
            )
        }

        declarationStorage.leaveScope(symbol)
        return irConstructor
    }

    fun createIrLazyClass(
        firClass: FirRegularClass,
        irParent: IrDeclarationParent,
        symbol: IrClassSymbol
    ): Fir2IrLazyClass = firClass.convertWithOffsets { startOffset, endOffset ->
        val firClassOrigin = firClass.irOrigin(c)
        Fir2IrLazyClass(c, startOffset, endOffset, firClassOrigin, firClass, symbol, irParent)
    }

    fun createIrLazyTypeAlias(
        firTypeAlias: FirTypeAlias,
        irParent: IrDeclarationParent,
        symbol: IrTypeAliasSymbol
    ): Fir2IrLazyTypeAlias = firTypeAlias.convertWithOffsets { startOffset, endOffset ->
        Fir2IrLazyTypeAlias(
            c, startOffset, endOffset, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB, firTypeAlias, symbol, irParent
        )
    }

    // TODO: Should be private
    fun createIrLazyField(
        fir: FirField,
        symbol: IrFieldSymbol,
        lazyParent: IrDeclarationParent,
        declarationOrigin: IrDeclarationOrigin,
        irPropertySymbol: IrPropertySymbol?
    ): Fir2IrLazyField {
        return fir.convertWithOffsets { startOffset, endOffset ->
            Fir2IrLazyField(
                c, startOffset, endOffset, declarationOrigin, fir, (lazyParent as? Fir2IrLazyClass)?.fir, symbol, irPropertySymbol
            ).apply {
                parent = lazyParent
            }
        }
    }

    fun createIrPropertyForPureField(
        fir: FirField,
        fieldSymbol: IrFieldSymbol,
        irPropertySymbol: IrPropertySymbol,
        lazyParent: IrDeclarationParent,
        declarationOrigin: IrDeclarationOrigin
    ): IrProperty {
        val field = createIrLazyField(fir, fieldSymbol, lazyParent, declarationOrigin, irPropertySymbol)
        return Fir2IrLazyPropertyForPureField(c, field, irPropertySymbol, lazyParent)
    }
}

internal fun FirCallableDeclaration.isFakeOverride(firContainingClass: FirRegularClass?): Boolean {
    val declaration = unwrapUseSiteSubstitutionOverrides()
    return declaration.isSubstitutionOrIntersectionOverride ||
            firContainingClass?.symbol?.toLookupTag() != declaration.containingClassLookupTag()
}
