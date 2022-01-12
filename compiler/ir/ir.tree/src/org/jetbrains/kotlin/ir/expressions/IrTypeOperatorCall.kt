/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrType

abstract class IrTypeOperatorCall : IrExpression() {
    abstract val operator: IrTypeOperator
    abstract var argument: IrExpression
    abstract var typeOperand: IrType
    abstract val typeOperandClassifier: IrClassifierSymbol
}
