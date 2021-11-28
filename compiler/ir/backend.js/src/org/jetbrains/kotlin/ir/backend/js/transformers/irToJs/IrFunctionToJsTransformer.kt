/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.js.backend.ast.JsFunction

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrFunctionToJsTransformer : BaseIrElementToJsNodeTransformer<JsFunction, JsGenerationContext> {
    override fun visitSimpleFunction(declaration: IrSimpleFunction, context: JsGenerationContext): JsFunction {
        val parentClass = declaration.parent as? IrClass
        val isInterfaceDefaultImpl = parentClass?.isInterface ?: false
        val funcName = if (declaration.dispatchReceiverParameter == null || isInterfaceDefaultImpl) {
            if (declaration.parent is IrFunction) {
                context.getNameForValueDeclaration(declaration)
            } else {
                context.getNameForStaticFunction(declaration)
            }
        } else {
            context.getNameForMemberFunction(declaration)
        }
        return translateFunction(declaration, funcName, context)
    }

    override fun visitConstructor(declaration: IrConstructor, context: JsGenerationContext): JsFunction {
        assert(declaration.isPrimary)
        val funcName = context.getNameForConstructor(declaration)
        return translateFunction(declaration, funcName, context)
    }
}