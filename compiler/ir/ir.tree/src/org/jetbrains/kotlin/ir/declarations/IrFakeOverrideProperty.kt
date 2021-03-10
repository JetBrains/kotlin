/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol

interface IrFakeOverrideProperty : IrDeclaration {
    override val symbol: IrPropertySymbol

    var modality: Modality
    var getter: IrSimpleFunction?
    var setter: IrSimpleFunction?

    fun acquireSymbol(symbol: IrPropertySymbol): IrProperty

    val isBound: Boolean
}
