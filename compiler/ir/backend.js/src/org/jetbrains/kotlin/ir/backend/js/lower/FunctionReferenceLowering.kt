/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

class FunctionReferenceLowering(val context: JsIrBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(FunctionReferenceCollector())
        irFile.transformChildrenVoid(LambdaFunctionVisitor())
        irFile.transformChildrenVoid(FunctionReferenceVisitor())
        lambdaImpls.forEach { it.transformChildrenVoid(FunctionReferenceVisitor()) }
    }

    val lambdas = mutableMapOf<IrFunction, KotlinType>()
    val oldToNewDeclMap = mutableMapOf<IrSymbolOwner, IrFunction>()
    val lambdaImpls = mutableListOf<IrFunction>()

    inner class FunctionReferenceCollector : IrElementVisitorVoid {
        override fun visitFunctionReference(expression: IrFunctionReference) {
            lambdas[expression.symbol.owner as IrFunction] = expression.type
        }

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }
    }

    inner class FunctionReferenceVisitor : IrElementTransformerVoid() {
        override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
            val newTarget = oldToNewDeclMap[expression.symbol.owner]

            return if (newTarget != null) IrCallImpl(expression.startOffset, expression.endOffset, newTarget.symbol).apply {
                copyTypeArgumentsFrom(expression)
                var index = 0
                for (i in 0 until expression.valueArgumentsCount) {
                    val arg = expression.getValueArgument(i)
                    if (arg != null) {
                        putValueArgument(index++, arg)
                    }
                }
            } else expression
        }

    }

    inner class LambdaFunctionVisitor : IrElementTransformerVoid() {
        override fun visitFunction(declaration: IrFunction): IrStatement {
            if (declaration !in lambdas.keys) return declaration

            // transform
            // foo$lambda(a: A, b: B, c: closure$C, d: closure$D) { /*body*/ } ===>
            // foo$lambda(c: closure$C, d: closure$D) {
            //     var $funcRef = function(a: A, b: B) { /*body*/ }
            //     return $funcRef
            // }

            val functionType = lambdas[declaration]!!

            // FunctionN<T1, T2, ..., TN, TReturn>, arguments.size = N + 1

            val lambdaParamCount =
                if (declaration.extensionReceiverParameter == null) functionType.arguments.size - 1 else functionType.arguments.size - 2

            assert(lambdaParamCount >= 0)

            val descriptor = SimpleFunctionDescriptorImpl.create(
                declaration.descriptor.containingDeclaration,
                Annotations.EMPTY,
                Name.identifier("${declaration.descriptor.name}_impl"),
                declaration.descriptor.kind,
                declaration.descriptor.source
            )
            descriptor.initialize(
                null,
                declaration.descriptor.dispatchReceiverParameter,
//                declaration.descriptor.typeParameters,
                emptyList(),
                declaration.descriptor.valueParameters.drop(declaration.valueParameters.size - lambdaParamCount).mapIndexed { index, valueParameterDescriptor ->
                    valueParameterDescriptor.copy(descriptor, valueParameterDescriptor.name, index)
                },
                declaration.descriptor.returnType,
                declaration.descriptor.modality,
                declaration.descriptor.visibility
            )

            val symbol = IrSimpleFunctionSymbolImpl(descriptor)
            val func = IrFunctionImpl(declaration.startOffset, declaration.endOffset, declaration.origin, symbol).apply {
                dispatchReceiverParameter = declaration.dispatchReceiverParameter
                extensionReceiverParameter = declaration.extensionReceiverParameter
                declaration.valueParameters.drop(declaration.valueParameters.size - lambdaParamCount).forEachIndexed { index, param ->
                    valueParameters += IrValueParameterImpl(
                        param.startOffset,
                        param.endOffset,
                        param.origin,
                        descriptor.valueParameters[index]
                    )
                }
            }

            val newDeclDescriptor = SimpleFunctionDescriptorImpl.create(
                declaration.descriptor.containingDeclaration,
                declaration.descriptor.annotations,
                declaration.descriptor.name,
                declaration.descriptor.kind,
                declaration.descriptor.source
            ).initialize(
                null,
                declaration.descriptor.dispatchReceiverParameter,
//                declaration.descriptor.typeParameters,
                emptyList(),
                declaration.descriptor.valueParameters.dropLast(lambdaParamCount),
                functionType,
                declaration.descriptor.modality,
                declaration.descriptor.visibility
            )
            val newDeclSymbol = IrSimpleFunctionSymbolImpl(newDeclDescriptor)

            val funcReference = IrFunctionReferenceImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, functionType, symbol, descriptor, null, null)
            val varDescriptor = IrTemporaryVariableDescriptorImpl(newDeclDescriptor, Name.identifier("\$funRef"), functionType)
            val closureStmt = IrVariableImpl(
                declaration.startOffset,
                declaration.endOffset,
                JsLoweredDeclarationOrigin.JS_LAMBDA_CREATION,
                varDescriptor,
                funcReference
            )
            val returnStmt = IrReturnImpl(
                declaration.startOffset,
                declaration.endOffset,
                symbol,
                IrGetValueImpl(declaration.startOffset, declaration.endOffset, IrVariableSymbolImpl(varDescriptor))
            )

            val oldBody = declaration.body

            val newBody = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(closureStmt, returnStmt))
            func.body = oldBody

            lambdaImpls += func

            val newDeclaration = IrFunctionImpl(declaration.startOffset, declaration.endOffset, declaration.origin, newDeclSymbol).apply {
                body = newBody
                valueParameters += declaration.valueParameters.dropLast(lambdaParamCount)
            }

            oldToNewDeclMap[declaration] = newDeclaration

            return newDeclaration
        }
    }
}