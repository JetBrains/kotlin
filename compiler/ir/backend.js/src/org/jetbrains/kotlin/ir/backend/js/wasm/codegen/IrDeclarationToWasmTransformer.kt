/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm.codegen

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.js.backend.ast.*

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrDeclarationToWasmTransformer : BaseIrElementToWasmNodeTransformer<JsStatement, WasmStaticContext> {

    override fun visitSimpleFunction(declaration: IrSimpleFunction, context: WasmStaticContext): JsStatement {
        require(!declaration.descriptor.isExpect)
        return declaration.accept(IrFunctionToWasmTransformer(), context).makeStmt()
    }

    override fun visitConstructor(declaration: IrConstructor, context: WasmStaticContext): JsStatement {
        return declaration.accept(IrFunctionToWasmTransformer(), context).makeStmt()
    }

    override fun visitClass(declaration: IrClass, context: WasmStaticContext): JsStatement {
        return WasmClassGenerator(declaration, context).generate()
    }

    override fun visitField(declaration: IrField, context: WasmStaticContext): JsStatement {
        val fieldName = context.getNameForField(declaration)

        if (declaration.isExternal) return JsEmpty

        if (declaration.initializer != null) {
            val initializer = declaration.initializer!!.accept(IrElementToWasmExpressionTransformer(), context)
            context.initializerBlock.statements += jsAssignment(fieldName.makeRef(), initializer).makeStmt()
        }

        return JsVars(JsVars.JsVar(fieldName))
    }

    override fun visitVariable(declaration: IrVariable, context: WasmStaticContext): JsStatement {
        return declaration.accept(IrElementToWasmStatementTransformer(), context)
    }
}
