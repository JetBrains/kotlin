/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.implicitCastTo
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.IrTypeSubstitutor
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.extractTypeParameters
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.addFakeOverrides
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.createDispatchReceiverParameterWithClassParent
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.nonDispatchParameters
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import kotlin.collections.plus

abstract class AbstractFunctionReferenceLowering<C: CommonBackendContext>(val context: C) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transform(object : IrTransformer<IrDeclarationParent>() {
            override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclarationParent): IrStatement {
                return super.visitDeclaration(declaration, declaration as? IrDeclarationParent ?: data)
            }

            override fun visitRichFunctionReference(expression: IrRichFunctionReference, data: IrDeclarationParent): IrExpression {
                expression.transformChildren(this, data)
                val irBuilder = context.createIrBuilder((data as IrSymbolOwner).symbol,
                                                        expression.startOffset, expression.endOffset)

                val clazz = buildClass(expression, data)
                val constructor = clazz.primaryConstructor!!
                val newExpression = irBuilder.irCallConstructor(constructor.symbol, emptyList()).apply {
                    origin = getConstructorCallOrigin(expression)
                    for ((index, value) in expression.boundValues.withIndex()) {
                        arguments[index] = value
                    }
                    for (index in expression.boundValues.size until arguments.size) {
                        arguments[index] = irBuilder.getExtraConstructorArgument(constructor.parameters[index], expression)
                    }
                }
                return irBuilder.irBlock {
                    +clazz
                    +newExpression
                }
            }

            override fun visitFunctionReference(expression: IrFunctionReference, data: IrDeclarationParent): IrExpression {
                shouldNotBeCalled()
            }
        }, data = irFile)
    }

    // Sam class used as superclass can sometimes have type projections.
    // But that's not suitable for super-types, so we erase them
    private fun IrType.removeProjections(): IrType {
        if (this !is IrSimpleType) return this
        val arguments = arguments.mapIndexed { index, argument ->
            if (argument is IrTypeProjection && argument.variance == Variance.INVARIANT)
                argument.type
            else
                (classifier as IrClassSymbol).owner.typeParameters[index].erasedUpperBound.defaultType
        }
        return classifier.typeWith(arguments)
    }

    private fun buildClass(functionReference: IrRichFunctionReference, parent: IrDeclarationParent): IrClass {
        val functionReferenceClass = context.irFactory.buildClass {
            startOffset = functionReference.startOffset
            endOffset = functionReference.endOffset
            origin = getClassOrigin(functionReference)
            name = getReferenceClassName(functionReference)
            visibility = DescriptorVisibilities.LOCAL
        }.apply {
            this.parent = parent
            createThisReceiverParameter()
        }
        val superClass = getSuperClassType(functionReference)
        val superInterfaceType = functionReference.type.removeProjections()
        functionReferenceClass.superTypes = mutableListOf(superClass, superInterfaceType)
        val constructor = functionReferenceClass.addConstructor {
            this.startOffset = functionReference.startOffset
            this.endOffset = functionReference.endOffset
            origin = getConstructorOrigin(functionReference)
            isPrimary = true
        }.apply {
            body = context.createIrBuilder(symbol, this.startOffset, this.endOffset).irBlockBody {
                +generateSuperClassConstructorCall(superClass, functionReference)
                +IrInstanceInitializerCallImpl(this.startOffset, this.endOffset, functionReferenceClass.symbol, context.irBuiltIns.unitType)
            }
            parameters = functionReference.boundValues.mapIndexed { index, value ->
                buildValueParameter(this) {
                    name = Name.identifier("p${index}")
                    startOffset = value.startOffset
                    endOffset = value.endOffset
                    type = value.type
                    kind = IrParameterKind.Regular
                }
            } + getExtraConstructorParameters(this, functionReference)
        }

        val fields = functionReference.boundValues.mapIndexed { index, captured ->
            functionReferenceClass.addField {
                startOffset = captured.startOffset
                endOffset = captured.endOffset
                name = Name.identifier("f${'$'}${index}")
                visibility = DescriptorVisibilities.PRIVATE
                isFinal = true
                type = captured.type
            }.apply {
                val builder = context.createIrBuilder(symbol, startOffset, endOffset)
                initializer = builder.irExprBody(builder.irGet(constructor.parameters[index]))
            }
        }
        buildInvokeMethod(
                functionReference,
                functionReferenceClass,
                superInterfaceType,
                fields
        ).apply {
            postprocessInvoke(this, functionReference)
        }

        generateExtraMethods(functionReferenceClass, functionReference)

        functionReferenceClass.addFakeOverrides(
                context.typeSystem,
                // Built function overrides originalSuperMethod, while, if parent class is already lowered, it would
                // transformedSuperMethod in its declaration list. We need not fake override in that case.
                // Later lowerings will fix it and replace function with one overriding transformedSuperMethod.
                ignoredParentSymbols = listOf(functionReference.overriddenFunctionSymbol)
        )
        postprocessClass(functionReferenceClass, functionReference)
        return functionReferenceClass
    }

    private fun buildInvokeMethod(
        functionReference: IrRichFunctionReference,
        functionReferenceClass: IrClass,
        superInterfaceType: IrType,
        boundFields: List<IrField>
    ): IrSimpleFunction {
        val superFunction = functionReference.overriddenFunctionSymbol.owner
        val invokeFunction = functionReference.invokeFunction
        return functionReferenceClass.addFunction {
            startOffset = functionReference.startOffset
            endOffset = functionReference.endOffset
            origin = getInvokeMethodOrigin(functionReference)
            name = superFunction.name
            returnType = invokeFunction.returnType
            isSuspend = superFunction.isSuspend
        }.apply {
            attributeOwnerId = functionReference.attributeOwnerId

            parameters += createDispatchReceiverParameterWithClassParent()
            require(superFunction.typeParameters.isEmpty()) { "Fun interface abstract function can't have type parameters" }

            val typeSubstitutor = IrTypeSubstitutor(
                extractTypeParameters(superInterfaceType.classOrFail.owner).map { it.symbol },
                (superInterfaceType as IrSimpleType).arguments,
                allowEmptySubstitution = true
            )

            val nonDispatchParameters = superFunction.nonDispatchParameters.map {
                it.copyTo(this, type = typeSubstitutor.substitute(it.type), defaultValue = null)
            }
            this.parameters += nonDispatchParameters
            overriddenSymbols += superFunction.symbol

            val builder = context.createIrBuilder(symbol)
            body = builder.irBlockBody {
                val variablesMapping = buildMap {
                    for ((index, field) in boundFields.withIndex()) {
                        put(invokeFunction.parameters[index], irTemporary(irGetField(irGet(dispatchReceiverParameter!!), field)))
                    }
                    for ((index, parameter) in nonDispatchParameters.withIndex()) {
                        val invokeParameter = invokeFunction.parameters[index + boundFields.size]
                        if (parameter.type != invokeParameter.type) {
                            put(invokeParameter, irTemporary(irGet(parameter).implicitCastTo(invokeParameter.type)))
                        } else {
                            put(invokeParameter, parameter)
                        }
                    }
                }
                val transformedBody = invokeFunction.body!!.transform(object : VariableRemapper(variablesMapping) {
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        if (expression.returnTargetSymbol == invokeFunction.symbol) {
                            expression.returnTargetSymbol = this@apply.symbol
                        }
                        return super.visitReturn(expression)
                    }

                    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
                        if (declaration.parent == invokeFunction)
                            declaration.parent = this@apply
                        return super.visitDeclaration(declaration)
                    }
                }, null)
                when (transformedBody) {
                    is IrBlockBody -> +transformedBody.statements
                    is IrExpressionBody -> +irReturn(transformedBody.expression)
                    else -> error("Unexpected body type: ${transformedBody::class.simpleName}")
                }
            }
        }
    }

    protected open fun postprocessClass(functionReferenceClass: IrClass, functionReference: IrRichFunctionReference) {}
    protected open fun postprocessInvoke(invokeFunction: IrSimpleFunction, functionReference: IrRichFunctionReference) {}
    protected open fun generateExtraMethods(functionReferenceClass: IrClass, reference: IrRichFunctionReference) {}
    protected open fun getExtraConstructorParameters(constructor: IrConstructor, reference: IrRichFunctionReference): List<IrValueParameter> = emptyList()
    protected open fun IrBuilderWithScope.getExtraConstructorArgument(parameter: IrValueParameter, reference: IrRichFunctionReference): IrExpression? = null
    protected abstract fun IrBuilderWithScope.generateSuperClassConstructorCall(superClassType: IrType, functionReference: IrRichFunctionReference) : IrDelegatingConstructorCall

    protected abstract fun getReferenceClassName(reference: IrRichFunctionReference): Name
    protected abstract fun getSuperClassType(reference: IrRichFunctionReference) : IrType
    protected abstract fun getClassOrigin(reference: IrRichFunctionReference) : IrDeclarationOrigin
    protected abstract fun getConstructorOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin
    protected abstract fun getInvokeMethodOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin
    protected abstract fun getConstructorCallOrigin(reference: IrRichFunctionReference): IrStatementOrigin?
}