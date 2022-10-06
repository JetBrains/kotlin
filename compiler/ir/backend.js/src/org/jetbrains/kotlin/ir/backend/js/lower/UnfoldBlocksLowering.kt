/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.isFalseConst
import org.jetbrains.kotlin.ir.util.isTrueConst
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class UnfoldBlocksLowering(context: JsIrBackendContext) : BodyLoweringPass {
    private val transformer = UnfoldBlocksLoweringVisitor(context)

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transform(transformer, null)
    }
}

private class UnfoldBlocksLoweringVisitor(val context: JsIrBackendContext) : IrElementTransformerVoid() {
    override fun visitBlock(expression: IrBlock): IrExpression {
        expression.statements.unfold()
        return super.visitBlock(expression)
    }

    override fun visitBlockBody(body: IrBlockBody): IrBody {
        body.statements.unfold()
        return super.visitBlockBody(body)
    }

    override fun visitBody(body: IrBody): IrBody {
        if (body is IrBlockBody) {
            body.statements.unfold()
        }
        return super.visitBody(body)
    }

    override fun visitComposite(expression: IrComposite): IrExpression {
        expression.statements.unfold()
        return super.visitComposite(expression)
    }

    private fun MutableList<IrStatement>.unfold() {
        var i = 0;
        while (i < size) {
            val item = get(i)
            if (item is IrGetValue || item.isUnitInstanceFunction() || item.isUnreachableFunction()) {
                removeAt(i)
            } else if (item is IrBlock || item is IrComposite){
                val statements = when (item) {
                    is IrBlock -> item.statements
                    is IrComposite -> item.statements
                    else -> break
                }
                removeAt(i)
                addAll(i, statements)
            } else {
                i++
            }
        }
    }

    private fun IrStatement.isUnreachableFunction(): Boolean {
        val symbol = (this as? IrCall)?.symbol ?: return false
        return symbol == context.intrinsics.unreachable
    }

    private fun IrStatement.isUnitInstanceFunction(): Boolean {
        val owner = (this as? IrCall)?.symbol?.owner ?: return false
        return owner.origin === JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION &&
                owner.returnType.classifierOrNull === context.irBuiltIns.unitClass
    }
}