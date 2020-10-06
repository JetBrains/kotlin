/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.js.backend.ast.*

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrDeclarationToJsTransformer : BaseIrElementToJsNodeTransformer<JsStatement, JsGenerationContext> {

    override fun visitSimpleFunction(declaration: IrSimpleFunction, context: JsGenerationContext): JsStatement {
        require(!declaration.isExpect)
        return declaration.accept(IrFunctionToJsTransformer(), context).makeStmt()
    }

    override fun visitConstructor(declaration: IrConstructor, context: JsGenerationContext): JsStatement {
        return declaration.accept(IrFunctionToJsTransformer(), context).makeStmt()
    }

    override fun visitClass(declaration: IrClass, context: JsGenerationContext): JsStatement {
        return JsClassGenerator(
            declaration,
            context.newDeclaration()
        ).generate()
    }

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: JsGenerationContext): JsStatement {
        // To avoid compiler crash with UnimplementedException just in case I added this visitor to catch uncovered cases
        return JsSingleLineComment("\$error code: declaration")
    }

    override fun visitField(declaration: IrField, context: JsGenerationContext): JsStatement {
        val fieldName = context.getNameForField(declaration)

        if (declaration.isExternal) return JsEmpty

        if (declaration.initializer != null) {
            val initializer = declaration.initializer!!.accept(IrElementToJsExpressionTransformer(), context)
            context.staticContext.initializerBlock.statements += jsAssignment(fieldName.makeRef(), initializer).makeStmt()
        }

        return JsVars(JsVars.JsVar(fieldName))
    }

    override fun visitVariable(declaration: IrVariable, context: JsGenerationContext): JsStatement {
        return declaration.accept(IrElementToJsStatementTransformer(), context)
    }

    override fun visitScript(irScript: IrScript, context: JsGenerationContext): JsStatement {
        return JsGlobalBlock().apply {
            statements += irScript.declarations.map { it.accept(this@IrDeclarationToJsTransformer, context) }
            statements += irScript.statements.map { it.accept(IrElementToJsStatementTransformer(), context) }
        }
    }
}
