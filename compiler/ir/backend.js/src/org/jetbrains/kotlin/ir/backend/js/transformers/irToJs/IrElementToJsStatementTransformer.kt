/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.constructedClass
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.js.backend.ast.*

class IrElementToJsStatementTransformer : BaseIrElementToJsNodeTransformer<JsStatement, JsGenerationContext> {

    // TODO: is it right place for this logic? Or should it be implemented as a separate lowering?
    override fun visitTypeOperator(expression: IrTypeOperatorCall, context: JsGenerationContext): JsStatement {
        if (expression.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {
            return expression.argument.accept(this, context)
        }
        return super.visitTypeOperator(expression, context)
    }

    override fun visitBlockBody(body: IrBlockBody, context: JsGenerationContext): JsStatement {
        return JsBlock(body.statements.map { it.accept(this, context) })
    }

    override fun visitBlock(expression: IrBlock, context: JsGenerationContext): JsBlock {
        return JsBlock(expression.statements.map { it.accept(this, context) })
    }

    override fun visitComposite(expression: IrComposite, context: JsGenerationContext): JsStatement {
        // TODO introduce JsCompositeBlock?
        return JsBlock(expression.statements.map { it.accept(this, context) })
    }

    override fun visitExpression(expression: IrExpression, context: JsGenerationContext): JsStatement {
        return JsExpressionStatement(expression.accept(IrElementToJsExpressionTransformer(), context))
    }

    override fun visitBreak(jump: IrBreak, context: JsGenerationContext): JsStatement {
        return JsBreak(jump.label?.let(::JsNameRef))
    }

    override fun visitContinue(jump: IrContinue, context: JsGenerationContext): JsStatement {
        return JsContinue(jump.label?.let(::JsNameRef))
    }

    override fun visitReturn(expression: IrReturn, context: JsGenerationContext): JsStatement {
        return JsReturn(expression.value.accept(IrElementToJsExpressionTransformer(), context))
    }

    override fun visitThrow(expression: IrThrow, context: JsGenerationContext): JsStatement {
        return JsThrow(expression.value.accept(IrElementToJsExpressionTransformer(), context))
    }

    override fun visitVariable(declaration: IrVariable, context: JsGenerationContext): JsStatement {
        val varName = context.getNameForSymbol(declaration.symbol)
        return jsVar(varName, declaration.initializer, context)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, context: JsGenerationContext): JsStatement {
        if (KotlinBuiltIns.isAny(expression.symbol.constructedClass)) {
            return JsEmpty
        }
        return expression.accept(IrElementToJsExpressionTransformer(), context).makeStmt()
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, context: JsGenerationContext): JsStatement {

        // TODO: implement
        return JsEmpty
    }

    override fun visitWhen(expression: IrWhen, context: JsGenerationContext): JsStatement {
        return expression.toJsNode(this, context, ::JsIf) ?: JsEmpty
    }

    override fun visitWhileLoop(loop: IrWhileLoop, context: JsGenerationContext): JsStatement {
        //TODO what if body null?
        return JsWhile(loop.condition.accept(IrElementToJsExpressionTransformer(), context), loop.body?.accept(this, context))
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, context: JsGenerationContext): JsStatement {
        //TODO what if body null?
        return JsDoWhile(loop.condition.accept(IrElementToJsExpressionTransformer(), context), loop.body?.accept(this, context))
    }
}
