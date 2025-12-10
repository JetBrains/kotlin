/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.transformers.correspondingStatic
import org.jetbrains.kotlin.ir.backend.js.lower.transformers.transformMemberToStaticFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * Extracts private members from classes.
 */
class PrivateMembersLowering(val context: JsIrBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        return when (declaration) {
            is IrSimpleFunction -> declaration.transformPrivateFunctionToStaticFunction()
                ?.also { declaration.correspondingStatic = it }
                ?.let(::listOf)

            is IrProperty -> listOf(declaration.apply {
                // Detach old function from corresponding property
                this.getter = this.getter?.let(::transformAccessor)
                this.setter = this.setter?.let(::transformAccessor)
            })
            else -> null
        }
    }

    private fun transformAccessor(accessor: IrSimpleFunction) =
        accessor.transformPrivateFunctionToStaticFunction() ?: accessor

    private fun IrSimpleFunction.transformPrivateFunctionToStaticFunction(): IrSimpleFunction? =
        runIf(visibility == DescriptorVisibilities.PRIVATE && dispatchReceiverParameter != null) {
            with(context) {
                transformMemberToStaticFunction(this@transformPrivateFunctionToStaticFunction)
            }
        }
}

class PrivateMemberBodiesLowering(val context: JsIrBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transform(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)

                return expression.symbol.owner.correspondingStatic?.let {
                    transformPrivateToStaticCall(expression, it)
                } ?: expression
            }

            private fun transformPrivateToStaticCall(expression: IrCall, staticTarget: IrSimpleFunction): IrCall {
                val newExpression = IrCallImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    staticTarget.symbol,
                    typeArgumentsCount = expression.typeArguments.size,
                    origin = expression.origin,
                    superQualifierSymbol = expression.superQualifierSymbol
                )

                newExpression.arguments.assignFrom(expression.arguments)
                newExpression.copyTypeArgumentsFrom(expression)

                return newExpression
            }
        }, null)
    }
}
