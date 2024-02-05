/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrExpression

class IrCatchImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var catchParameter: IrVariable,
) : IrCatch() {
    constructor(
        startOffset: Int,
        endOffset: Int,
        catchParameter: IrVariable,
        result: IrExpression
    ) : this(startOffset, endOffset, catchParameter) {
        this.result = result
    }

    override lateinit var result: IrExpression
}