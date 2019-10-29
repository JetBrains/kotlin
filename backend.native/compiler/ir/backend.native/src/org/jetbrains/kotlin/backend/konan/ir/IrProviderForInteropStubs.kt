/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyFunction
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyProperty
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyTypeAlias
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.descriptorUtil.module

/**
 * Generates external IR declarations for descriptors from interop libraries.
 */
class IrProviderForInteropStubs : LazyIrProvider {

    override lateinit var declarationStubGenerator: DeclarationStubGenerator

    override fun getDeclaration(symbol: IrSymbol): IrLazyDeclarationBase? =
            if (symbol.descriptor.module.isFromInteropLibrary()) {
                provideIrDeclaration(symbol)
            } else {
                null
            }

    private fun provideIrDeclaration(symbol: IrSymbol): IrLazyDeclarationBase = when (symbol) {
        is IrSimpleFunctionSymbol -> provideIrFunction(symbol)
        is IrPropertySymbol -> provideIrProperty(symbol)
        is IrTypeAliasSymbol -> provideIrTypeAlias(symbol)
        else -> error("Unsupported interop declaration: symbol=$symbol, descriptor=${symbol.descriptor}")
    }

    private fun provideIrFunction(symbol: IrSimpleFunctionSymbol): IrLazyFunction =
            declarationStubGenerator.symbolTable.declareSimpleFunction(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                    symbol.descriptor, this::createFunctionDeclaration
            ) as IrLazyFunction

    private fun createFunctionDeclaration(symbol: IrSimpleFunctionSymbol) =
            IrLazyFunction(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                    symbol, declarationStubGenerator, declarationStubGenerator.typeTranslator
            )

    private fun provideIrProperty(symbol: IrPropertySymbol): IrLazyProperty =
            declarationStubGenerator.symbolTable.declareProperty(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                    symbol.descriptor, propertyFactory = this::createPropertyDeclaration
            ) as IrLazyProperty

    private fun createPropertyDeclaration(symbol: IrPropertySymbol) =
            IrLazyProperty(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                    symbol, declarationStubGenerator, declarationStubGenerator.typeTranslator, null
            )

    private fun provideIrTypeAlias(symbol: IrTypeAliasSymbol): IrLazyTypeAlias =
            declarationStubGenerator.symbolTable.declareTypeAlias(
                    symbol.descriptor, this::createTypeAlias
            ) as IrLazyTypeAlias

    private fun createTypeAlias(symbol: IrTypeAliasSymbol): IrLazyTypeAlias =
            IrLazyTypeAlias(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                    symbol, symbol.descriptor.name,
                    symbol.descriptor.visibility, symbol.descriptor.isActual,
                    declarationStubGenerator, declarationStubGenerator.typeTranslator
            )
}