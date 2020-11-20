/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.declarations.IrVariable

abstract class IrSuspensionPoint : IrExpression() {
    abstract var suspensionPointIdParameter: IrVariable
    abstract var result: IrExpression
    abstract var resumeResult: IrExpression
}

abstract class IrSuspendableExpression : IrExpression() {
    abstract var suspensionPointId: IrExpression
    abstract var result: IrExpression
}
