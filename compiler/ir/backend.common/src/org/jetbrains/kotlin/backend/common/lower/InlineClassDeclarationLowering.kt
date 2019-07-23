/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.createStaticFunctionWithReceivers
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

private const val INLINE_CLASS_IMPL_SUFFIX = "-impl"

// TODO: Support incremental compilation
class InlineClassLowering(val context: BackendContext) {
    private val transformedFunction = mutableMapOf<IrFunctionSymbol, IrSimpleFunctionSymbol>()

    val inlineClassDeclarationLowering = object : ClassLoweringPass {
        override fun lower(irClass: IrClass) {
            if (!irClass.isInline) return

            irClass.transformDeclarationsFlat { declaration ->
                when (declaration) {
                    is IrConstructor -> listOf(transformConstructor(declaration))
                    is IrSimpleFunction -> transformMethodFlat(declaration)
                    is IrProperty -> listOf(declaration)  // Getters and setters should be flattened
                    is IrField -> listOf(declaration)
                    is IrClass -> listOf(declaration)
                    else -> error("Unexpected declaration: $declaration")
                }
            }
        }

        private fun transformConstructor(irConstructor: IrConstructor): IrDeclaration {
            if (irConstructor.isPrimary) return irConstructor

            // Secondary constructors are lowered into static function
            val result = transformedFunction.getOrPut(irConstructor.symbol) { createStaticBodilessMethod(irConstructor).symbol }.owner
            val irClass = irConstructor.parentAsClass

            // Copied and adapted from Kotlin/Native InlineClassTransformer
            result.body = context.createIrBuilder(result.symbol).irBlockBody(result) {

                // Secondary ctors of inline class must delegate to some other constructors.
                // Use these delegating call later to initialize this variable.
                lateinit var thisVar: IrVariable
                val parameterMapping = result.valueParameters.associateBy {
                    irConstructor.valueParameters[it.index].symbol
                }

                (irConstructor.body as IrBlockBody).statements.forEach { statement ->
                    +statement.transform(object : IrElementTransformerVoid() {
                        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                            expression.transformChildrenVoid()
                            return irBlock(expression) {
                                thisVar = createTmpVariable(
                                    expression,
                                    irType = irClass.defaultType
                                )
                                thisVar.parent = result
                            }
                        }

                        override fun visitGetValue(expression: IrGetValue): IrExpression {
                            expression.transformChildrenVoid()
                            if (expression.symbol == irClass.thisReceiver?.symbol) {
                                return irGet(thisVar)
                            }

                            parameterMapping[expression.symbol]?.let { return irGet(it) }
                            return expression
                        }

                        override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                            declaration.transformChildrenVoid(this)
                            if (declaration.parent == irConstructor)
                                declaration.parent = result
                            return declaration
                        }

                        override fun visitReturn(expression: IrReturn): IrExpression {
                            expression.transformChildrenVoid()
                            if (expression.returnTargetSymbol == irConstructor.symbol) {
                                return irReturn(irBlock(expression.startOffset, expression.endOffset) {
                                    +expression.value
                                    +irGet(thisVar)
                                })
                            }

                            return expression
                        }

                    }, null)
                }
                +irReturn(irGet(thisVar))
            }

            return result
        }


        private fun transformMethodFlat(function: IrSimpleFunction): List<IrDeclaration> {
            // TODO: Support fake-overridden methods without boxing
            if (function.isStaticMethodOfClass || !function.isReal)
                return listOf(function)

            val staticMethod = createStaticBodilessMethod(function)
            transformedFunction[function.symbol] = staticMethod.symbol

            // Move function body to static method, transforming value parameters and nested declarations
            function.body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                    declaration.transformChildrenVoid(this)
                    if (declaration.parent == function)
                        declaration.parent = staticMethod

                    return declaration
                }

                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    val valueDeclaration = expression.symbol.owner as? IrValueParameter ?: return super.visitGetValue(expression)

                    return context.createIrBuilder(staticMethod.symbol).irGet(
                        when (valueDeclaration) {
                            function.dispatchReceiverParameter, function.parentAsClass.thisReceiver ->
                                staticMethod.valueParameters[0]

                            function.extensionReceiverParameter ->
                                staticMethod.valueParameters[1]

                            in function.valueParameters -> {
                                val offset = if (function.extensionReceiverParameter != null) 2 else 1
                                staticMethod.valueParameters[valueDeclaration.index + offset]
                            }

                            else -> return expression
                        }
                    )
                }
            })

            staticMethod.body = function.body

            if (function.overriddenSymbols.isEmpty())  // Function is used only in unboxed context
                return listOf(staticMethod)

            // Delegate original function to static implementation
            function.body = context.createIrBuilder(function.symbol).irBlockBody {
                +irReturn(
                    irCall(staticMethod).apply {
                        val parameters =
                            listOfNotNull(
                                function.dispatchReceiverParameter!!,
                                function.extensionReceiverParameter
                            ) + function.valueParameters

                        for ((index, valueParameter) in parameters.withIndex()) {
                            putValueArgument(index, irGet(valueParameter))
                        }
                    }
                )
            }

            return listOf(function, staticMethod)
        }
    }

    val inlineClassUsageLowering = object : FileLoweringPass {

        override fun lower(irFile: IrFile) {
            irFile.transformChildrenVoid(object : IrElementTransformerVoid() {

                override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
                    expression.transformChildrenVoid(this)
                    val function = expression.symbol.owner
                    if (!function.parentAsClass.isInline || function.isPrimary) {
                        return expression
                    }

                    return irCall(expression, getOrCreateStaticMethod(function))
                }

                override fun visitCall(expression: IrCall): IrExpression {
                    expression.transformChildrenVoid(this)
                    val function = expression.symbol.owner
                    if (function.parent !is IrClass ||
                        function.isStaticMethodOfClass ||
                        !function.parentAsClass.isInline ||
                        (function is IrSimpleFunction && !function.isReal) ||
                        (function is IrConstructor && function.isPrimary)
                    ) {
                        return expression
                    }

                    return irCall(
                        expression,
                        getOrCreateStaticMethod(function),
                        receiversAsArguments = (function is IrSimpleFunction)
                    )
                }

                override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                    expression.transformChildrenVoid(this)
                    val function = expression.symbol.owner
                    val klass = function.parentAsClass
                    return when {
                        !klass.isInline -> expression
                        function.isPrimary -> irConstructorCall(expression, function)
                        else -> irCall(expression, getOrCreateStaticMethod(function))
                    }
                }

                private fun getOrCreateStaticMethod(function: IrFunction): IrSimpleFunctionSymbol =
                    transformedFunction.getOrPut(function.symbol) {
                        createStaticBodilessMethod(function).also {
                            function.parentAsClass.declarations.add(it)
                        }.symbol
                    }
            })
        }
    }

    private fun Name.toInlineClassImplementationName() = when {
        isSpecial -> Name.special(asString() + INLINE_CLASS_IMPL_SUFFIX)
        else -> Name.identifier(asString() + INLINE_CLASS_IMPL_SUFFIX)
    }

    private fun createStaticBodilessMethod(function: IrFunction): IrSimpleFunction =
        createStaticFunctionWithReceivers(function.parent, function.name.toInlineClassImplementationName(), function, copyBody = false)
}
