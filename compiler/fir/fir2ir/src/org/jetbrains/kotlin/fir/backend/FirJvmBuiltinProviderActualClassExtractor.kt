/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSymbolProvider
import org.jetbrains.kotlin.ir.ActualClassExtractor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.classIdOrFail
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.name.StandardClassIds

class FirJvmBuiltinProviderActualClassExtractor(
    val provider: FirBuiltinSymbolProvider,
    val classifierStorage: Fir2IrClassifierStorage,
) : ActualClassExtractor() {
    companion object {
        val ActualizeByJvmBuiltinProviderFqName = StandardClassIds.Annotations.ActualizeByJvmBuiltinProvider.asSingleFqName()
    }

    override fun extract(expectIrClass: IrClass): IrClassSymbol? {
        if (expectIrClass.annotations.none { it.isAnnotation(ActualizeByJvmBuiltinProviderFqName) }) return null

        val regularClassSymbol = provider.getRegularClassSymbolByClassId(expectIrClass.classIdOrFail) ?: return null
        return classifierStorage.getIrClassSymbol(regularClassSymbol)
    }
}