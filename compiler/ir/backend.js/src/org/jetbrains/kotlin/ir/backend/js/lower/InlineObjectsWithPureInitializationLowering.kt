/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.isObjectInstanceGetter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.irError
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class InlineObjectsWithPureInitializationLowering(val context: JsCommonBackendContext) : BodyLoweringPass {
    private val IrClass.instanceField by context.mapping.objectToInstanceField
    private val IrClass.hasPureInitialization by context.mapping.objectsWithPureInitialization

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (!expression.symbol.owner.isObjectInstanceGetter()) return super.visitCall(expression)
                val objectToCreate = expression.symbol.owner.returnType.classOrNull?.owner
                    ?: irError("Expect return type of an object getter is an object type") {
                        withIrEntry("expression", expression)
                    }
                if (objectToCreate.hasPureInitialization != true || objectToCreate.isCompanion) return super.visitCall(expression)
                val instanceFieldForObject = objectToCreate.instanceField
                    ?: irError("An instance field for an object should exist") {
                        withIrEntry("objectToCreate", objectToCreate)
                        withIrEntry("expression", expression)
                    }
                return JsIrBuilder.buildGetField(
                    instanceFieldForObject.symbol,
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    origin = expression.origin
                )
            }
        })
    }
}
