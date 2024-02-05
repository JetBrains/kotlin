/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.types.IrType

class IrSuspensionPointImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var suspensionPointIdParameter: IrVariable,
    override var result: IrExpression,
    override var resumeResult: IrExpression
) : IrSuspensionPoint()

