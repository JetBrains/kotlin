/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.declarations.IrVariable


interface IrSuspensionPoint : IrExpression {
    var suspensionPointIdParameter: IrVariable
    var result: IrExpression
    var resumeResult: IrExpression
}

interface IrSuspendableExpression : IrExpression {
    var suspensionPointId: IrExpression
    var result: IrExpression
}
