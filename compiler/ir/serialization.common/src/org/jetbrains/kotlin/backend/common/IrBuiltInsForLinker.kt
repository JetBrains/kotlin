/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.IrBuiltInsOverSymbolFinder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds

class IrBuiltInsForLinker(
    linker: KotlinIrLinker,
    override val languageVersionSettings: LanguageVersionSettings,
) : IrBuiltInsOverSymbolFinder(SymbolFinderOverLinker(linker)) {
    override val irFactory: IrFactory = linker.symbolTable.irFactory

    override val operatorsPackageFragment: IrExternalPackageFragment = createEmptyExternalPackageFragment(
        fqName = StandardClassIds.BASE_INTERNAL_IR_PACKAGE
    )
    override val kotlinInternalPackageFragment: IrExternalPackageFragment = createEmptyExternalPackageFragment(
        fqName = StandardClassIds.BASE_INTERNAL_PACKAGE
    )

    override val ieee754equalsFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> = notImplemented()

    override val eqeqeqSymbol: IrSimpleFunctionSymbol = notImplemented()

    override val eqeqSymbol: IrSimpleFunctionSymbol = notImplemented()

    override val throwCceSymbol: IrSimpleFunctionSymbol = notImplemented()

    override val throwIseSymbol: IrSimpleFunctionSymbol = notImplemented()

    override val andandSymbol: IrSimpleFunctionSymbol = notImplemented()

    override val ororSymbol: IrSimpleFunctionSymbol = notImplemented()

    override val noWhenBranchMatchedExceptionSymbol: IrSimpleFunctionSymbol = notImplemented()

    override val illegalArgumentExceptionSymbol: IrSimpleFunctionSymbol = notImplemented()

    override val dataClassArrayMemberHashCodeSymbol: IrSimpleFunctionSymbol = notImplemented()

    override val dataClassArrayMemberToStringSymbol: IrSimpleFunctionSymbol = notImplemented()

    override val checkNotNullSymbol: IrSimpleFunctionSymbol = notImplemented()

    override val linkageErrorSymbol: IrSimpleFunctionSymbol = notImplemented()

    override val lessFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> = notImplemented()

    override val lessOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> = notImplemented()

    override val greaterOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> = notImplemented()

    override val greaterFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> = notImplemented()

    private fun createEmptyExternalPackageFragment(fqName: FqName): IrExternalPackageFragment =
        IrExternalPackageFragmentImpl(
            IrExternalPackageFragmentSymbolImpl(), fqName
        )

    private fun notImplemented(): Nothing = error("Should be taken from linker")
}