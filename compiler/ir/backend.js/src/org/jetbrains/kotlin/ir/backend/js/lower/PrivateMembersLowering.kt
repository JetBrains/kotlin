/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrPropertyReferenceImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

private val STATIC_THIS_PARAMETER by IrDeclarationOriginImpl

private var IrFunction.correspondingStatic: IrSimpleFunction? by irAttribute(followAttributeOwner = false)

/**
 * Extracts private members from classes.
 */
class PrivateMembersLowering(val context: JsIrBackendContext) : DeclarationTransformer {

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        return when (declaration) {
            is IrSimpleFunction -> transformMemberToStaticFunction(declaration)?.let { staticFunction ->
                declaration.correspondingStatic = staticFunction
                listOf(staticFunction)
            }
            is IrProperty -> listOf(declaration.apply {
                // Detach old function from corresponding property
                this.getter = this.getter?.let { g -> transformAccessor(g) }
                this.setter = this.setter?.let { s -> transformAccessor(s) }
            })
            else -> null
        }
    }

    private fun transformAccessor(accessor: IrSimpleFunction) = transformMemberToStaticFunction(accessor) ?: accessor

    private fun transformMemberToStaticFunction(function: IrSimpleFunction): IrSimpleFunction? {

        if (function.visibility != DescriptorVisibilities.PRIVATE || function.dispatchReceiverParameter == null) return null
        val newVisibility = if (function.isLocal) DescriptorVisibilities.LOCAL else function.visibility

        val staticFunction = context.irFactory.buildFun {
            updateFrom(function)
            name = function.name
            returnType = function.returnType
            visibility = newVisibility
        }.also {
            it.parent = function.parent
            it.annotations = function.annotations
        }

        function.correspondingStatic = staticFunction

        staticFunction.typeParameters = function.typeParameters.map { it.deepCopyWithSymbols(staticFunction) }
        staticFunction.parameters = function.parameters.map { originalParameter ->
            if (originalParameter.kind == IrParameterKind.DispatchReceiver) {
                originalParameter.copyTo(
                    staticFunction,
                    origin = STATIC_THIS_PARAMETER,
                    name = Name.identifier("\$this"),
                    kind = IrParameterKind.Regular,
                )
            } else {
                // TODO better way to avoid copying default value
                originalParameter.copyTo(staticFunction, defaultValue = null, kind = IrParameterKind.Regular)
            }
        }

        val parameterMapping = function.parameters.zip(staticFunction.parameters).toMap()

        val parameterTransformer = object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrGetValue = parameterMapping[expression.symbol.owner]?.let {
                expression.run { IrGetValueImpl(startOffset, endOffset, type, it.symbol, origin) }
            } ?: expression
        }

        fun IrBody.copyWithParameters(): IrBody {
            return deepCopyWithSymbols(staticFunction).also {
                it.transform(parameterTransformer, null)
            }
        }

        function.parameters.forEach {
            // TODO better way to avoid copying default value

            parameterMapping[it]?.apply {
                it.defaultValue?.let { originalDefault ->
                    defaultValue = context.irFactory.createExpressionBody(
                        startOffset = it.startOffset,
                        endOffset = it.endOffset,
                        expression = (originalDefault.copyWithParameters() as IrExpressionBody).expression,
                    )
                }
            }
        }

        function.body?.let {
            staticFunction.body = when (it) {
                is IrBlockBody -> context.irFactory.createBlockBody(it.startOffset, it.endOffset) {
                    statements += (it.copyWithParameters() as IrBlockBody).statements
                }
                is IrExpressionBody -> context.irFactory.createExpressionBody(
                    startOffset = it.startOffset,
                    endOffset = it.endOffset,
                    expression = (it.copyWithParameters() as IrExpressionBody).expression,
                )
                is IrSyntheticBody -> it
            }
        }

        return staticFunction
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

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                super.visitFunctionReference(expression)

                return expression.symbol.owner.correspondingStatic?.let {
                    transformPrivateToStaticReference(expression) {
                        IrFunctionReferenceImpl(
                            expression.startOffset, expression.endOffset,
                            expression.type,
                            it.symbol, expression.typeArguments.size,
                            expression.reflectionTarget, expression.origin
                        )
                    }
                } ?: expression
            }

            override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
                super.visitPropertyReference(expression)

                val staticGetter = expression.getter?.owner?.correspondingStatic
                val staticSetter = expression.setter?.owner?.correspondingStatic

                return if (staticGetter != null || staticSetter != null) {
                    transformPrivateToStaticReference(expression) {
                        IrPropertyReferenceImpl(
                            expression.startOffset, expression.endOffset,
                            expression.type,
                            expression.symbol, // TODO remap property symbol based on remapped getter/setter?
                            expression.typeArguments.size,
                            expression.field,
                            staticGetter?.symbol ?: expression.getter,
                            staticSetter?.symbol ?: expression.setter,
                            expression.origin
                        )
                    }
                } else expression
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

            private fun transformPrivateToStaticReference(
                expression: IrCallableReference<*>,
                builder: () -> IrCallableReference<*>
            ): IrCallableReference<*> {

                val newExpression = builder()

                newExpression.arguments.assignFrom(expression.arguments)
                newExpression.copyTypeArgumentsFrom(expression)

                return newExpression
            }
        }, null)
    }
}
