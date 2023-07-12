/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody

@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // TODO(KTIJ-26314): Remove this suppression
fun IrFactory.createExpressionBody(expression: IrExpression): IrExpressionBody =
    createExpressionBody(expression.startOffset, expression.endOffset, expression)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // TODO(KTIJ-26314): Remove this suppression
fun IrFactory.createBlockBody(
    startOffset: Int,
    endOffset: Int,
    initializer: IrBlockBody.() -> Unit,
): IrBlockBody = createBlockBody(startOffset, endOffset).apply(initializer)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // TODO(KTIJ-26314): Remove this suppression
fun IrFactory.createBlockBody(
    startOffset: Int,
    endOffset: Int,
    statements: List<IrStatement>,
): IrBlockBody = createBlockBody(startOffset, endOffset) { this.statements.addAll(statements) }