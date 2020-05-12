/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol

interface IrSimpleFunction :
    IrFunction,
    IrSymbolDeclaration<IrSimpleFunctionSymbol>,
    IrOverridableDeclaration<IrSimpleFunctionSymbol>,
    IrOverridableMember,
    IrAttributeContainer {

    val isTailrec: Boolean
    val isSuspend: Boolean
    val isFakeOverride: Boolean
    val isOperator: Boolean

    var correspondingPropertySymbol: IrPropertySymbol?
}

val IrFunction.isPropertyAccessor: Boolean
    get() = this is IrSimpleFunction && correspondingPropertySymbol != null