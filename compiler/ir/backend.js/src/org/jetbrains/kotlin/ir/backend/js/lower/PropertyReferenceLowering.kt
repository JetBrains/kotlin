/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.memoryOptimizedMap

class PropertyReferenceLowering(private val context: JsIrBackendContext) : BodyLoweringPass {

    private val referenceBuilderSymbol = context.kpropertyBuilder
    private val localDelegateBuilderSymbol = context.klocalDelegateBuilder
    private val jsClassSymbol = context.intrinsics.jsClass

    private val throwISE = context.ir.symbols.throwISE

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val currentParent = container as? IrDeclarationParent ?: container.parent
        val newDeclarations = PropertyReferenceTransformer(currentParent).process(irBody)
        if (!newDeclarations.isEmpty()) {
            val file = container.file
            newDeclarations.forEach { it.parent = file }
            file.declarations.addAll(newDeclarations)
        }
    }

    private inner class PropertyReferenceTransformer(var currentParent: IrDeclarationParent) : IrElementTransformerVoid() {

        val newDeclarations = mutableListOf<IrDeclaration>()

        fun process(irBody: IrBody): List<IrDeclaration> {
            irBody.transformChildrenVoid(this)
            return newDeclarations
        }

        private fun buildFactoryFunction(reference: IrPropertyReference): IrSimpleFunction {
            val property = reference.symbol.owner

            val factoryDeclaration = context.irFactory.buildFun {
                startOffset = reference.startOffset
                endOffset = reference.endOffset
                returnType = reference.type
                name = Name.identifier("${property.name.asString()}\$factory")
                origin = PROPERTY_REFERENCE_FACTORY
            }

            val boundArguments = listOfNotNull(reference.dispatchReceiver, reference.extensionReceiver)

            val valueParameters = boundArguments.mapIndexed { i, arg ->
                buildValueParameter(factoryDeclaration) {
                    type = arg.type
                    index = i
                    name = Name.identifier("\$b$i")
                }
            }
            factoryDeclaration.valueParameters = valueParameters

            // TODO: type parameters

            // 0 - name
            // 1 - paramCount
            // 2 - type
            // 3 - getter
            // 4 - setter

            val arity = (reference.type as IrSimpleType).arguments.size - 1

            val irBuilder = context.createIrBuilder(factoryDeclaration.symbol)
            factoryDeclaration.body = irBuilder.irBlockBody {
                +irReturn(irCall(referenceBuilderSymbol).apply {
                    putValueArgument(0, reference.nameExpression())
                    putValueArgument(1, irInt(arity))
                    putValueArgument(2, reference.getJsTypeConstructor())
                    putValueArgument(3, buildGetterLambda(factoryDeclaration, reference, valueParameters))
                    putValueArgument(4, buildSetterLambda(factoryDeclaration, reference, valueParameters))
                })
            }

            newDeclarations.add(factoryDeclaration)

            return factoryDeclaration
        }

        private fun buildGetterLambda(factory: IrSimpleFunction, reference: IrPropertyReference, boundValueParameters: List<IrValueParameter>): IrExpression {
            val getter = reference.getter?.owner
                ?: compilationException(
                    "Getter expected",
                    reference
                )
            return buildAccessorLambda(factory, getter, reference, boundValueParameters)
        }

        private fun buildSetterLambda(factory: IrSimpleFunction, reference: IrPropertyReference, boundValueParameters: List<IrValueParameter>): IrExpression {
            val setter = reference.run {
                setter?.owner ?: return IrConstImpl.constNull(startOffset, endOffset, context.irBuiltIns.nothingNType)
            }

            return buildAccessorLambda(factory, setter, reference, boundValueParameters)
        }

        private fun buildAccessorLambda(factory: IrSimpleFunction, accessor: IrSimpleFunction, reference: IrPropertyReference, boundValueParameters: List<IrValueParameter>): IrExpression {
            val superName = when (accessor.symbol) {
                reference.getter -> "get"
                reference.setter -> "set"
                else -> compilationException(
                    "Unexpected accessor",
                    accessor
                )
            }

            val classifier = (reference.type as IrSimpleType).classOrNull
                ?: compilationException(
                    "Simple type expected",
                    reference
                )
            val supperAccessor =
                classifier.owner.declarations.filterIsInstance<IrSimpleFunction>().single { it.name.asString() == superName }

            val function = context.irFactory.buildFun {
                startOffset = reference.startOffset
                endOffset = reference.endOffset
                returnType = supperAccessor.returnType
                name = supperAccessor.name
            }

            function.parent = factory

            val unboundValueParameters = supperAccessor.valueParameters.memoryOptimizedMap { it.copyTo(function) }
            function.valueParameters = unboundValueParameters
            val arity = unboundValueParameters.size
            val total = arity + boundValueParameters.size

            var b = 0
            var u = 0

            val irBuilder = context.createIrBuilder(function.symbol)
            function.body = irBuilder.irBlockBody {
                val irAccessorCall = irCall(accessor.symbol)

                if (accessor.dispatchReceiverParameter != null) {
                    irAccessorCall.dispatchReceiver =
                        if (reference.dispatchReceiver != null) irGet(boundValueParameters[b++]) else irGet(unboundValueParameters[u++])
                }

                if (accessor.extensionReceiverParameter != null) {
                    irAccessorCall.extensionReceiver =
                        if (reference.extensionReceiver != null) irGet(boundValueParameters[b++]) else irGet(unboundValueParameters[u++])
                }

                if (u < unboundValueParameters.size) {
                    irAccessorCall.putValueArgument(0, irGet(unboundValueParameters[u++]))
                }

                assert(u == arity)
                assert((u + b) == total)

                +irReturn(irAccessorCall)
            }

            return IrFunctionExpressionImpl(
                reference.startOffset,
                reference.endOffset,
                context.irBuiltIns.anyType,
                function,
                IrStatementOrigin.LAMBDA
            )
        }

        private fun IrPropertyReference.nameExpression(): IrExpression {
            val propertyName = symbol.owner.name.asString()
            return IrConstImpl.string(startOffset, endOffset, context.irBuiltIns.stringType, propertyName)
        }

        private fun IrExpression.getJsTypeConstructor(): IrExpression {
            val irCall = IrCallImpl(startOffset, endOffset, jsClassSymbol.owner.returnType, jsClassSymbol, 1, 0)
            irCall.putTypeArgument(0, type)
            return irCall
        }

        override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
            expression.transformChildrenVoid(this)

            val factoryFunction = buildFactoryFunction(expression)

            assert(expression.valueArgumentsCount == 0)

            return IrCallImpl(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                factoryFunction.symbol,
                expression.typeArgumentsCount,
                factoryFunction.valueParameters.size
            ).apply {
                for (ti in 0 until typeArgumentsCount) {
                    putTypeArgument(ti, expression.getTypeArgument(ti))
                }

                var vi = 0
                expression.dispatchReceiver?.let { putValueArgument(vi++, it) }
                expression.extensionReceiver?.let { putValueArgument(vi++, it) }
            }
        }

        override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
            expression.transformChildrenVoid(this)

            val builderCall = expression.run {
                IrCallImpl(startOffset, endOffset, type, localDelegateBuilderSymbol, typeArgumentsCount, 4)
            }

            val localName = expression.symbol.owner.name.asString()
            val isMutable = expression.setter != null

            // 0 - name
            // 1 - type
            // 2 - isMutable
            // 3 - lambda

            expression.run {
                builderCall.putValueArgument(0, IrConstImpl.string(startOffset, endOffset, context.irBuiltIns.stringType, localName))
                builderCall.putValueArgument(1, expression.getJsTypeConstructor())
                builderCall.putValueArgument(2, IrConstImpl.boolean(startOffset, endOffset, context.irBuiltIns.booleanType, isMutable))
                builderCall.putValueArgument(3, buildLocalDelegateLambda(expression))
            }

            return builderCall
        }

        private fun buildLocalDelegateLambda(expression: IrLocalDelegatedPropertyReference): IrExpression {
            val delegatedVar = expression.delegate.owner
            val function = context.irFactory.buildFun {
                startOffset = expression.startOffset
                endOffset = expression.endOffset
                returnType = context.irBuiltIns.nothingType
                name = Name.identifier("${delegatedVar.name}\$stub")
            }

            function.parent = currentParent

            function.body = with(context.createIrBuilder(function.symbol)) {
                irBlockBody {
                    +irReturn(irCall(throwISE))
                }
            }

            return expression.run {
                IrFunctionExpressionImpl(startOffset, endOffset, context.irBuiltIns.anyType, function, IrStatementOrigin.LAMBDA)
            }
        }
    }

    companion object {
        val PROPERTY_REFERENCE_FACTORY = IrDeclarationOriginImpl("PROPERTY_REFERNCE_FACTORY")
    }
}
