/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isInlineFunWithReifiedParameter
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeSubstitutor
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.setDeclarationsParent
import org.jetbrains.kotlin.ir.util.typeSubstitutionMap
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf

// Replace callable reference on inline function with reified parameter
// with callable reference on new non inline function with substituted types
class WrapInlineDeclarationsWithReifiedTypeParametersLowering(val context: BackendContext) : BodyLoweringPass {
    private val irFactory
        get() = context.irFactory

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildren(object : IrElementTransformer<IrDeclarationParent?> {
            override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclarationParent?) =
                super.visitDeclaration(declaration, declaration as? IrDeclarationParent ?: data)

            override fun visitFunctionReference(expression: IrFunctionReference, data: IrDeclarationParent?): IrExpression {
                expression.transformChildren(this, data)

                val owner = expression.symbol.owner as? IrSimpleFunction
                    ?: return expression

                if (!owner.isInlineFunWithReifiedParameter()) {
                    return expression
                }
                @Suppress("UNCHECKED_CAST")
                val typeSubstitutor = IrTypeSubstitutor(expression.typeSubstitutionMap as Map<IrTypeParameterSymbol, IrTypeArgument>)

                val function = irFactory.buildFun {
                    name = Name.identifier("${owner.name}${"$"}wrap")
                    returnType = typeSubstitutor.substitute(owner.returnType)
                    visibility = DescriptorVisibilities.LOCAL
                    origin = IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE
                    startOffset = SYNTHETIC_OFFSET
                    endOffset = SYNTHETIC_OFFSET
                }.apply {
                    parent = data ?: error("Unable to get a proper parent while lower ${expression.render()} at ${container.render()}")
                    val irBuilder = context.createIrBuilder(symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET)
                    val forwardExtensionReceiverAsParam = owner.extensionReceiverParameter?.let { extensionReceiver ->
                        runIf(expression.extensionReceiver == null) {
                            addValueParameter(
                                extensionReceiver.name,
                                typeSubstitutor.substitute(extensionReceiver.type)
                            )
                            true
                        }
                    } ?: false
                    owner.valueParameters.forEach { valueParameter ->
                        addValueParameter(
                            valueParameter.name,
                            typeSubstitutor.substitute(valueParameter.type)
                        )
                    }
                    body = irFactory.createBlockBody(
                        expression.startOffset,
                        expression.endOffset
                    ) {
                        statements.add(
                            irBuilder.irReturn(
                                irBuilder.irCall(owner.symbol).also { call ->
                                    expression.extensionReceiver?.setDeclarationsParent(this@apply)
                                    expression.dispatchReceiver?.setDeclarationsParent(this@apply)
                                    val (extensionReceiver, forwardedParams) = if (forwardExtensionReceiverAsParam) {
                                        irBuilder.irGet(valueParameters.first()) to valueParameters.subList(1, valueParameters.size)
                                    } else {
                                        expression.extensionReceiver to valueParameters
                                    }
                                    call.extensionReceiver = extensionReceiver
                                    call.dispatchReceiver = expression.dispatchReceiver

                                    forwardedParams.forEachIndexed { index, valueParameter ->
                                        call.putValueArgument(index, irBuilder.irGet(valueParameter))
                                    }
                                    for (i in 0 until expression.typeArgumentsCount) {
                                        call.putTypeArgument(i, expression.getTypeArgument(i))
                                    }
                                },
                            )
                        )
                    }
                }
                return context.createIrBuilder(container.symbol).irBlock(
                    expression,
                    origin = IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE
                ) {
                    +function
                    +IrFunctionReferenceImpl.fromSymbolOwner(
                        expression.startOffset,
                        expression.endOffset,
                        expression.type,
                        function.symbol,
                        function.typeParameters.size,
                        expression.reflectionTarget
                    )
                }
            }
        }, container as? IrDeclarationParent)
    }
}
