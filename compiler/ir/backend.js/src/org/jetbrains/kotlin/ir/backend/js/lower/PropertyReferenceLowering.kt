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
import org.jetbrains.kotlin.ir.util.nonDispatchParameters
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.memoryOptimizedMap

class PropertyReferenceLowering(private val context: JsIrBackendContext) : BodyLoweringPass {

    private val referenceBuilderSymbol = context.kpropertyBuilder
    private val localDelegateBuilderSymbol = context.klocalDelegateBuilder
    private val jsClassSymbol = context.intrinsics.jsClass

    private val throwISE = context.symbols.throwISE

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

            val boundArguments = reference.arguments.filterNotNull()

            val factoryParameters = boundArguments.mapIndexed { i, arg ->
                buildValueParameter(factoryDeclaration) {
                    type = arg.type
                    name = Name.identifier("\$b$i")
                    kind = IrParameterKind.Regular
                }
            }
            factoryDeclaration.parameters = factoryParameters

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
                    arguments[0] = reference.nameExpression()
                    arguments[1] = irInt(arity)
                    arguments[2] = reference.getJsTypeConstructor()
                    arguments[3] = buildGetterLambda(factoryDeclaration, reference, factoryParameters)
                    arguments[4] = buildSetterLambda(factoryDeclaration, reference, factoryParameters)
                })
            }

            newDeclarations.add(factoryDeclaration)

            return factoryDeclaration
        }

        private fun buildGetterLambda(
            factory: IrSimpleFunction,
            reference: IrPropertyReference,
            boundParameters: List<IrValueParameter>,
        ): IrExpression {
            val getter = reference.getter?.owner
                ?: compilationException(
                    "Getter expected",
                    reference
                )
            return buildAccessorLambda(factory, getter, reference, boundParameters)
        }

        private fun buildSetterLambda(
            factory: IrSimpleFunction,
            reference: IrPropertyReference,
            boundParameters: List<IrValueParameter>,
        ): IrExpression {
            val setter = reference.run {
                setter?.owner ?: return IrConstImpl.constNull(startOffset, endOffset, context.irBuiltIns.nothingNType)
            }

            return buildAccessorLambda(factory, setter, reference, boundParameters)
        }

        private fun buildAccessorLambda(
            factory: IrSimpleFunction,
            accessor: IrSimpleFunction,
            reference: IrPropertyReference,
            boundParameters: List<IrValueParameter>,
        ): IrExpression {
            val superName = when (accessor.symbol) {
                reference.getter -> OperatorNameConventions.GET
                reference.setter -> OperatorNameConventions.SET
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
            val superAccessor =
                classifier.owner.declarations.filterIsInstance<IrSimpleFunction>().single { it.name == superName }

            val function = context.irFactory.buildFun {
                startOffset = reference.startOffset
                endOffset = reference.endOffset
                returnType = superAccessor.returnType
                name = superAccessor.name
            }

            function.parent = factory

            val unboundParameters = superAccessor.nonDispatchParameters.memoryOptimizedMap { it.copyTo(function) }
            function.parameters = unboundParameters
            val arity = unboundParameters.size
            val total = arity + boundParameters.size

            var b = 0
            var u = 0

            val irBuilder = context.createIrBuilder(function.symbol)
            function.body = irBuilder.irBlockBody {
                val irAccessorCall = irCall(accessor.symbol)

                for (i in accessor.parameters.indices) {
                    val parameter = if (reference.arguments.getOrNull(i) != null)
                        boundParameters[b++]
                    else
                        unboundParameters[u++]
                    irAccessorCall.arguments[i] = irGet(parameter)
                }

                check(u == arity)
                check((u + b) == total)

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
            val irCall = IrCallImpl(
                startOffset, endOffset, jsClassSymbol.owner.returnType, jsClassSymbol,
                typeArgumentsCount = 1,
            )
            irCall.typeArguments[0] = type
            return irCall
        }

        override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
            expression.transformChildrenVoid(this)

            val factoryFunction = buildFactoryFunction(expression)
            return IrCallImpl(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                factoryFunction.symbol,
                expression.typeArguments.size
            ).apply {
                for (ti in typeArguments.indices) {
                    typeArguments[ti] = expression.typeArguments[ti]
                }

                arguments.clear()
                expression.arguments.filterNotNullTo(arguments)
            }
        }

        override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
            expression.transformChildrenVoid(this)

            val builderCall = expression.run {
                IrCallImpl(startOffset, endOffset, type, localDelegateBuilderSymbol, typeArguments.size)
            }

            val localName = expression.symbol.owner.name.asString()
            val isMutable = expression.setter != null

            // 0 - name
            // 1 - type
            // 2 - isMutable
            // 3 - lambda

            expression.run {
                builderCall.arguments[0] = IrConstImpl.string(startOffset, endOffset, context.irBuiltIns.stringType, localName)
                builderCall.arguments[1] = expression.getJsTypeConstructor()
                builderCall.arguments[2] = IrConstImpl.boolean(startOffset, endOffset, context.irBuiltIns.booleanType, isMutable)
                builderCall.arguments[3] = buildLocalDelegateLambda(expression)
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
