/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.isPrimary
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.js.backend.ast.*

class IrDeclarationToJsTransformer : BaseIrElementToJsNodeTransformer<JsStatement, JsGenerationContext> {

    override fun visitSimpleFunction(declaration: IrSimpleFunction, context: JsGenerationContext): JsStatement {
        return declaration.accept(IrFunctionToJsTransformer(), context).makeStmt()
    }

    override fun visitConstructor(declaration: IrConstructor, context: JsGenerationContext): JsStatement {
        return declaration.accept(IrFunctionToJsTransformer(), context).makeStmt()
    }

    override fun visitClass(declaration: IrClass, context: JsGenerationContext): JsStatement {
        return JsClassGenerator(declaration, context).generate()
    }

    override fun visitField(declaration: IrField, context: JsGenerationContext): JsStatement {
        val fieldName = context.getNameForSymbol(declaration.symbol)
        val initExpression =
            declaration.initializer?.accept(IrElementToJsExpressionTransformer(), context) ?: JsPrefixOperation(
                JsUnaryOperator.VOID,
                JsIntLiteral(1)
            )
        return jsAssignment(JsNameRef(fieldName, JsThisRef()), initExpression).makeStmt()
    }
}
