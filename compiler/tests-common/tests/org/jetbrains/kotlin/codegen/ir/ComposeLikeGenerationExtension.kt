/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 *
 *
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.codegen.ir

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class ComposeLikeExtensionRegistrar : ComponentRegistrar {
    companion object {
        fun registerComponents(project: Project) {
            IrGenerationExtension.registerExtension(project, ComposeLikeGenerationExtension())
        }
    }

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        registerComponents(project)
    }
}

class ComposeLikeGenerationExtension : IrGenerationExtension {
    private val rewrittenFunctions = mutableSetOf<IrFunction>()

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transformChildrenVoid(ComposeLikeDefaultArgumentRewriter(pluginContext, rewrittenFunctions))
        moduleFragment.transformChildrenVoid(ComposeLikeDefaultMethodCallRewriter(pluginContext, rewrittenFunctions))
    }
}

class ComposeLikeDefaultMethodCallRewriter(private val context: IrPluginContext, private val rewrittenFunctions: Set<IrFunction>) :
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
                function.valueParameters.size,
                expression.origin,
                expression.superQualifierSymbol
            ).also {
                it.dispatchReceiver = expression.dispatchReceiver?.transform(this, null)
                it.extensionReceiver = expression.extensionReceiver?.transform(this, null)
                var bitmap = 0
                for (i in function.valueParameters.indices) {
                    if (i < expression.valueArgumentsCount) {
                        if (expression.getValueArgument(i) != null) {
                            it.putValueArgument(i, expression.getValueArgument(i))
                        } else {
                            bitmap = bitmap or (1.shl(i))
                            it.putValueArgument(
                                i,
                                IrConstImpl.defaultValueForType(
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
                                    function.valueParameters[i].type
                                ).let { defaultValue ->
                                    IrCompositeImpl(
                                        defaultValue.startOffset,
                                        defaultValue.endOffset,
                                        defaultValue.type,
                                        IrStatementOrigin.DEFAULT_VALUE,
                                        listOf(defaultValue)
                                    )
                                }
                            )
                        }
                    }
                }
                it.putValueArgument(
                    function.valueParameters.size - 1,
                    IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.intType, bitmap)
                )
            }
        } else {
            super.visitCall(expression)
        }
    }
}

class ComposeLikeDefaultArgumentRewriter(
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
        val hasDefaultArguments = declaration.valueParameters.any { it.defaultValue != null }
        if (!hasDefaultArguments) return super.visitFunction(declaration)
        rewrittenFunctions.add(declaration)
        val newParameters = mutableListOf<IrValueParameter>()
        declaration.valueParameters.forEach { param ->
            newParameters.add(
                if (param.defaultValue != null) {
                    val result = context.irFactory.createValueParameter(
                        startOffset = param.startOffset,
                        endOffset = param.endOffset,
                        origin = param.origin,
                        name = param.name,
                        type = defaultParameterType(param),
                        isAssignable = param.defaultValue != null,
                        symbol = IrValueParameterSymbolImpl(),
                        index = param.index,
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
        declaration.valueParameters = newParameters
        val defaultParam = declaration.addValueParameter(
            "\$default",
            context.irBuiltIns.intType,
            IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION
        )
        declaration.transformChildrenVoid()
        val body = declaration.body!!
        val defaultSelection = mutableListOf<IrStatement>()
        declaration.valueParameters.forEach {
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
        return IrIfThenElseImpl(
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

    private fun irConst(value: Int): IrConst<Int> = IrConstImpl(
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
            lhs,
            null,
            rhs
        )
    }

    private fun irCall(
        symbol: IrFunctionSymbol,
        origin: IrStatementOrigin? = null,
        dispatchReceiver: IrExpression? = null,
        extensionReceiver: IrExpression? = null,
        vararg args: IrExpression
    ): IrCallImpl {
        return IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            symbol.owner.returnType,
            symbol as IrSimpleFunctionSymbol,
            symbol.owner.typeParameters.size,
            symbol.owner.valueParameters.size,
            origin
        ).also {
            if (dispatchReceiver != null) it.dispatchReceiver = dispatchReceiver
            if (extensionReceiver != null) it.extensionReceiver = extensionReceiver
            args.forEachIndexed { index, arg ->
                it.putValueArgument(index, arg)
            }
        }
    }

    private fun irNot(value: IrExpression): IrExpression {
        return irCall(
            context.irBuiltIns.booleanNotSymbol,
            dispatchReceiver = value
        )
    }

    private fun irEqual(lhs: IrExpression, rhs: IrExpression): IrExpression {
        return irCall(
            context.irBuiltIns.eqeqeqSymbol,
            null,
            null,
            null,
            lhs,
            rhs
        )
    }
}
