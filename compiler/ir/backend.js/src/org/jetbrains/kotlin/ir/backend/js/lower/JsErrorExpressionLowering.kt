/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.ErrorDeclarationLowering
import org.jetbrains.kotlin.backend.common.lower.ErrorExpressionLowering
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrErrorDeclaration
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.name.Name

class JsErrorDeclarationLowering(context: JsIrBackendContext) : ErrorDeclarationLowering() {
    private val nothingType = context.irBuiltIns.nothingType
    private val stringType = context.irBuiltIns.stringType
    private val errorSymbol = context.errorCodeSymbol
    private val irFactory = context.irFactory

    override fun transformErrorDeclaration(declaration: IrErrorDeclaration): IrDeclaration {
        require(errorSymbol != null) { "Should be non-null if errors are allowed" }
        return irFactory.buildFun {
            updateFrom(declaration)
            returnType = nothingType
            name = Name.identifier("\$errorDeclaration")
        }.also {
            it.parent = declaration.parent
            it.body = irFactory.createBlockBody(it.startOffset, it.endOffset) {
                statements += IrCallImpl(startOffset, endOffset, nothingType, errorSymbol, 0, 1, null, null).apply {
                    putValueArgument(0, IrConstImpl.string(startOffset, endOffset, stringType, "ERROR DECLARATION"))
                }
            }
        }
    }
}

class JsErrorExpressionLowering(context: JsIrBackendContext) : ErrorExpressionLowering(context) {

    private val stringType = context.irBuiltIns.nothingType
    private val errorSymbol = context.errorCodeSymbol

    override fun transformErrorExpression(expression: IrExpression, nodeKind: String): IrExpression {
        val errorExpression = expression as? IrErrorExpression
        val description = errorExpression?.let { "$nodeKind: ${it.description}" } ?: nodeKind
        return buildThrowError(expression, description)
    }

    private fun buildThrowError(element: IrExpression, description: String): IrExpression {
        require(errorSymbol != null) { "Should be non-null if errors are allowed" }
        return element.run {
            IrCallImpl(startOffset, endOffset, nothingType, errorSymbol, 0, 1, null, null).apply {
                putValueArgument(0, IrConstImpl.string(startOffset, endOffset, stringType, description))
            }
        }
    }
}