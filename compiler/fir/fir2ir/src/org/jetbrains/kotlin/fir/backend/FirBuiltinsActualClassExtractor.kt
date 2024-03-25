/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirBuiltinSymbolProvider
import org.jetbrains.kotlin.ir.ActualClassExtractor
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.name.ClassId

class FirBuiltinsActualClassExtractor(
    val provider: FirBuiltinSymbolProvider,
    val classifierStorage: Fir2IrClassifierStorage,
) : ActualClassExtractor() {
    override fun extract(classId: ClassId): IrClassSymbol? {
        val regularClassSymbol = provider.getRegularClassSymbolByClassId(classId) ?: return null
        return classifierStorage.getIrClassSymbol(regularClassSymbol)
    }
}