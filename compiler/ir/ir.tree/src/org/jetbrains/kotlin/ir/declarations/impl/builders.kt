/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

fun IrVariableImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrVariableSymbol,
    name: Name,
    type: IrType,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
): IrVariableImpl = IrVariableImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    origin = origin,
    symbol = symbol,
    name = name,
    type = type,
    isVar = isVar,
    isConst = isConst,
    isLateinit = isLateinit,
)