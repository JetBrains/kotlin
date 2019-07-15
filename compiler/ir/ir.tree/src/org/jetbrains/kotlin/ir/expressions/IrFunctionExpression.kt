/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

interface IrFunctionExpression : IrExpression {
    val origin: IrStatementOrigin
    var function: IrSimpleFunction
}