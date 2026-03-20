/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltInsOverSymbolFinder
import org.jetbrains.kotlin.ir.SymbolFinder
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds

@OptIn(InternalSymbolFinderAPI::class)
class IrBuiltInsForLinker(
    linker: KotlinIrLinker,
    override val languageVersionSettings: LanguageVersionSettings,
    symbolFinder: SymbolFinder = SymbolFinderOverLinker(linker)
) : IrBuiltInsOverSymbolFinder(symbolFinder) {
    override val irFactory: IrFactory = linker.symbolTable.irFactory

    override val operatorsPackageFragment: IrExternalPackageFragment = createEmptyExternalPackageFragment(
        fqName = StandardClassIds.BASE_INTERNAL_IR_PACKAGE
    )
    override val kotlinInternalPackageFragment: IrExternalPackageFragment = createEmptyExternalPackageFragment(
        fqName = StandardClassIds.BASE_INTERNAL_PACKAGE
    )

    private fun createEmptyExternalPackageFragment(fqName: FqName): IrExternalPackageFragment =
        IrExternalPackageFragmentImpl(
            IrExternalPackageFragmentSymbolImpl(), fqName
        )
}
