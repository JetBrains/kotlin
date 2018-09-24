/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.js.backend.ast.JsFunction

open class IrFunctionToJsTransformer : BaseIrElementToJsNodeTransformer<JsFunction, JsGenerationContext> {
    override fun visitSimpleFunction(declaration: IrSimpleFunction, context: JsGenerationContext): JsFunction {
        val funcName = context.getNameForSymbol(declaration.symbol)
        return translateFunction(declaration, funcName, false, context)
    }

    override fun visitConstructor(declaration: IrConstructor, context: JsGenerationContext): JsFunction {
        assert(declaration.isPrimary)
        val funcName = context.getNameForSymbol(declaration.symbol)
        val constructedClass = declaration.parent as IrClass
        return translateFunction(declaration, funcName, constructedClass.isObject, context)
    }
}