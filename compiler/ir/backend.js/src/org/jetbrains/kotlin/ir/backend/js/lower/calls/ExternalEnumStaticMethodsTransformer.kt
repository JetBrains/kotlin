/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.Name

class ExternalEnumStaticMethodsTransformerLowering(private val context: JsIrBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildren(object : IrElementTransformer<IrDeclaration> {
            override fun visitFunction(declaration: IrFunction, data: IrDeclaration): IrStatement {
                return super.visitFunction(declaration, declaration)
            }

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: IrDeclaration): IrElement {
                val call = super.visitFunctionAccess(expression, data)
                if (call !is IrFunctionAccessExpression) return call

                val owner = call.symbol.owner
                if (owner.origin != IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER || !owner.isExternal) return call
                val syntheticMethod = when {
                    owner.isValueOfMethod() -> context.intrinsics.externalEnumValueOfIntrinsic
                    owner.isValuesMethod() -> context.intrinsics.externalEnumValuesIntrinsic
                    else -> return call
                }
                return irCall(
                    call,
                    syntheticMethod,
                    newTypeArgumentsCount = call.typeArgumentsCount + 1
                ).withReifiedTypeReceiver(owner.parentAsClass)
            }
        }, container)
    }

    private fun IrFunctionAccessExpression.withReifiedTypeReceiver(irClass: IrClass): IrFunctionAccessExpression {
        return apply { putTypeArgument(0, irClass.defaultType) }
    }

    private fun IrFunction.isValueOfMethod(): Boolean {
        return name == Name.identifier("valueOf") &&
                valueParameters.count() == 1 &&
                valueParameters[0].type.isString()
    }

    private fun IrFunction.isValuesMethod(): Boolean {
        return name == Name.identifier("values") && valueParameters.isEmpty()
    }

}
