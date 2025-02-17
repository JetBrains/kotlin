/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.defaultValueForType
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import kotlin.collections.get

class ComposeLikeConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        IrGenerationExtension.registerExtension(ComposeLikeGenerationExtension())
    }
}

private class ComposeLikeGenerationExtension : IrGenerationExtension {
    private val rewrittenFunctions = mutableSetOf<IrFunction>()

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transformChildrenVoid(ComposeLikeDefaultArgumentRewriter(pluginContext, rewrittenFunctions))
        moduleFragment.transformChildrenVoid(ComposeLikeDefaultMethodCallRewriter(pluginContext, rewrittenFunctions))
    }
}

private class ComposeLikeDefaultMethodCallRewriter(private val context: IrPluginContext, private val rewrittenFunctions: Set<IrFunction>) :
    IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        val function = expression.symbol.owner
        return if (rewrittenFunctions.contains(function)) {
            IrCallImpl(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                expression.symbol,
                function.typeParameters.size,
                expression.origin,
                expression.superQualifierSymbol
            ).also {
                var bitmap = 0
                function.parameters.zip(expression.arguments).forEachIndexed { i, (parameter, argument) ->
                    if (argument != null) {
                        it.arguments[i] = argument.transform(this@ComposeLikeDefaultMethodCallRewriter, null)
                    } else {
                        bitmap = bitmap or (1.shl(i))
                        it.arguments[i] =
                            IrConstImpl.defaultValueForType(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                parameter.type
                            ).let { defaultValue ->
                                IrCompositeImpl(
                                    defaultValue.startOffset,
                                    defaultValue.endOffset,
                                    defaultValue.type,
                                    IrStatementOrigin.DEFAULT_VALUE,
                                    listOf(defaultValue)
                                )
                            }
                    }
                }
                it.arguments[function.parameters.lastIndex] =
                    IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.intType, bitmap)
            }
        } else {
            super.visitCall(expression)
        }
    }
}

private class ComposeLikeDefaultArgumentRewriter(
    private val context: IrPluginContext,
    private val rewrittenFunctions: MutableSet<IrFunction>
) : IrElementTransformerVoid() {
    private val parameterMapping = mutableMapOf<IrValueParameter, IrValueParameter>()

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        parameterMapping[expression.symbol.owner]?.let {
            return IrGetValueImpl(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                it.symbol,
                expression.origin
            )
        } ?: return super.visitGetValue(expression)
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val hasDefaultArguments = declaration.parameters.any { it.defaultValue != null }
        if (!hasDefaultArguments) return super.visitFunction(declaration)
        rewrittenFunctions.add(declaration)
        val newParameters = mutableListOf<IrValueParameter>()
        declaration.parameters.forEach { param ->
            newParameters.add(
                if (param.defaultValue != null) {
                    val result = context.irFactory.createValueParameter(
                        startOffset = param.startOffset,
                        endOffset = param.endOffset,
                        origin = param.origin,
                        kind = param.kind,
                        name = param.name,
                        type = defaultParameterType(param),
                        isAssignable = param.defaultValue != null,
                        symbol = IrValueParameterSymbolImpl(),
                        varargElementType = param.varargElementType,
                        isCrossinline = param.isCrossinline,
                        isNoinline = param.isNoinline,
                        isHidden = false,
                    ).also {
                        it.defaultValue = param.defaultValue
                        it.parent = declaration
                    }
                    parameterMapping[param] = result
                    result
                } else param
            )
        }
        declaration.parameters = newParameters
        val defaultParam = declaration.addValueParameter(
            "\$default",
            context.irBuiltIns.intType,
            IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION
        )
        declaration.transformChildrenVoid()
        val body = declaration.body!!
        val defaultSelection = mutableListOf<IrStatement>()
        declaration.parameters.forEach {
            if (it.hasDefaultValue()) {
                val index = defaultSelection.size
                defaultSelection.add(
                    irIf(
                        condition = irGetBit(defaultParam, index),
                        body = irSet(it, it.defaultValue!!.expression)
                    )
                )
                it.defaultValue = null
            }
        }

        declaration.body = context.irFactory.createBlockBody(
            body.startOffset,
            body.endOffset,
            listOf(
                *defaultSelection.toTypedArray(),
                *body.statements.toTypedArray()
            ),
        )
        return declaration
    }

    private fun defaultParameterType(param: IrValueParameter): IrType {
        val type = param.type
        return when {
            type.isPrimitiveType() -> type
            type.isInlineClassType() -> type
            else -> type.makeNullable()
        }
    }

    private fun irIf(condition: IrExpression, body: IrExpression): IrExpression {
        return IrWhenImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.unitType,
            origin = IrStatementOrigin.IF
        ).also {
            it.branches.add(
                IrBranchImpl(condition, body)
            )
        }
    }

    private fun irSet(variable: IrValueDeclaration, value: IrExpression): IrExpression {
        return IrSetValueImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.unitType,
            variable.symbol,
            value = value,
            origin = null
        )
    }

    private fun irNotEqual(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return irNot(irEqual(lhs, rhs))
    }

    private fun irGetBit(param: IrValueParameter, index: Int): IrExpression {
        // value and (1 shl index) != 0
        return irNotEqual(
            irAnd(
                // a value of 1 in default means it was NOT provided
                irGet(param),
                irConst(0b1 shl index)
            ),
            irConst(0)
        )
    }

    private fun irConst(value: Int): IrConst = IrConstImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.intType,
        IrConstKind.Int,
        value
    )

    private fun irGet(type: IrType, symbol: IrValueSymbol): IrExpression {
        return IrGetValueImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            symbol
        )
    }

    private fun irGet(variable: IrValueDeclaration): IrExpression {
        return irGet(variable.type, variable.symbol)
    }

    private fun IrType.binaryOperator(name: Name, paramType: IrType): IrFunctionSymbol =
        context.irBuiltIns.getBinaryOperator(name, this, paramType)

    private fun irAnd(lhs: IrExpression, rhs: IrExpression): IrCallImpl {
        return irCall(
            lhs.type.binaryOperator(Name.identifier("and"), rhs.type),
            null,
            lhs, // DispatchReceiver
            rhs, // Regular
        )
    }

    private fun irCall(
        symbol: IrFunctionSymbol,
        origin: IrStatementOrigin? = null,
        vararg args: IrExpression
    ): IrCallImpl {
        require(symbol.owner.parameters.size == args.size)
        return IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            symbol.owner.returnType,
            symbol as IrSimpleFunctionSymbol,
            symbol.owner.typeParameters.size,
            origin
        ).also {
            args.forEachIndexed { index, arg ->
                it.arguments[index] = arg
            }
        }
    }

    private fun irNot(value: IrExpression): IrExpression {
        return irCall(
            context.irBuiltIns.booleanNotSymbol,
            origin = null,
            value, // DispatchReceiver
        )
    }

    private fun irEqual(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return irCall(
            context.irBuiltIns.eqeqeqSymbol,
            null,
            lhs, // Regular
            rhs, // Regular
        )
    }
}
