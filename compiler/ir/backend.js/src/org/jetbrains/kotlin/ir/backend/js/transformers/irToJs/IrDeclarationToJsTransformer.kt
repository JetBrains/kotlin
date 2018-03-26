/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.isPrimary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.js.backend.ast.*

class IrDeclarationToJsTransformer : BaseIrElementToJsNodeTransformer<JsStatement, JsGenerationContext> {
    override fun visitProperty(declaration: IrProperty, context: JsGenerationContext): JsStatement {
        return jsVar(declaration.name, declaration.backingField?.initializer?.expression, context)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, context: JsGenerationContext): JsStatement {
        return declaration.accept(IrFunctionToJsTransformer(), context).makeStmt()
    }

    override fun visitConstructor(declaration: IrConstructor, context: JsGenerationContext): JsStatement {
        return declaration.accept(IrFunctionToJsTransformer(), context).makeStmt()
    }

    override fun visitClass(declaration: IrClass, context: JsGenerationContext): JsStatement {
        return JsClassGenerator(declaration, context).generate()
    }
}
