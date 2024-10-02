/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.utils

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class CodeFragmentConversionData(
    val classId: ClassId,
    val methodName: Name,
    val injectedValues: List<InjectedValue>,
)

class InjectedValue(val symbol: FirBasedSymbol<*>, val contextReceiverNumber: Int, val typeRef: FirTypeRef, val isMutated: Boolean) {
    val irParameterSymbol: IrValueParameterSymbol = IrValueParameterSymbolImpl()
}
