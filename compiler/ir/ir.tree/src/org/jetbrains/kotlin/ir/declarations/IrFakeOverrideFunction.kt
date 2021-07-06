/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol

interface IrFakeOverrideFunction : IrDeclaration {
    override val symbol: IrSimpleFunctionSymbol

    var modality: Modality

    fun acquireSymbol(symbol: IrSimpleFunctionSymbol): IrSimpleFunction

    val isBound: Boolean
}
