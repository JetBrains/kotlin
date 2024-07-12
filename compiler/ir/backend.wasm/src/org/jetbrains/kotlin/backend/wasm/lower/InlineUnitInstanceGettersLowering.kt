/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.isObjectInstanceGetter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class InlineUnitInstanceGettersLowering(val context: WasmBackendContext) : BodyLoweringPass {
    private val IrClass.instanceField by context.mapping.objectToInstanceField

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (!expression.symbol.owner.isObjectInstanceGetter()) return super.visitCall(expression)
                val objectToCreate = expression.symbol.owner.returnType.classOrNull?.owner
                    ?: error("Expect return type of an object getter is an object type")
                if (!objectToCreate.defaultType.isUnit()) return super.visitCall(expression)
                val unitInstance = objectToCreate.instanceField!!
                return JsIrBuilder.buildGetField(
                    unitInstance.symbol,
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    origin = expression.origin
                )
            }
        })
    }
}
