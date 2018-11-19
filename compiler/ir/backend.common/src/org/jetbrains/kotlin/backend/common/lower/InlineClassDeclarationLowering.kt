/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.toKotlinType
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
                val parameterMapping = result.valueParameters.associateBy { it ->
                    irConstructor.valueParameters[it.index].symbol
                }

                (irConstructor.body as IrBlockBody).statements.forEach { statement ->
                    +statement.transform(object : IrElementTransformerVoid() {
                        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                            expression.transformChildrenVoid()
                            return irBlock(expression) {
                                thisVar = irTemporary(
                                    expression,
                                    typeHint = irClass.defaultType.toKotlinType(),
                                    irType = irClass.defaultType
                                )
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

                    // TODO: Variable parents might not be initialized
                    if (declaration !is IrVariable && declaration.parent == function)
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
                                staticMethod.extensionReceiverParameter!!

                            in function.valueParameters ->
                                staticMethod.valueParameters[valueDeclaration.index + 1]

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
                            listOf(function.dispatchReceiverParameter!!) + function.valueParameters

                        for ((index, valueParameter) in parameters.withIndex()) {
                            putValueArgument(index, irGet(valueParameter))
                        }

                        extensionReceiver = function.extensionReceiverParameter?.let { irGet(it) }
                    }
                )
            }

            return listOf(function, staticMethod)
        }
    }

    val inlineClassUsageLowering = object : FileLoweringPass {

        override fun lower(irFile: IrFile) {
            irFile.transformChildrenVoid(object : IrElementTransformerVoid() {

                override fun visitCall(call: IrCall): IrExpression {
                    call.transformChildrenVoid(this)
                    val function = call.symbol.owner
                    if (
                        function.isDynamic() ||
                        function.parent !is IrClass ||
                        function.isStaticMethodOfClass ||
                        !function.parentAsClass.isInline ||
                        (function is IrSimpleFunction && !function.isReal) ||
                        (function is IrConstructor && function.isPrimary)
                    ) {
                        return call
                    }

                    return irCall(call, getOrCreateStaticMethod(function), dispatchReceiverAsFirstArgument = (function is IrSimpleFunction))
                }

                override fun visitDelegatingConstructorCall(call: IrDelegatingConstructorCall): IrExpression {
                    call.transformChildrenVoid(this)
                    val function = call.symbol.owner
                    val klass = function.parentAsClass
                    return when {
                        !klass.isInline -> call
                        function.isPrimary -> irCall(call, function)
                        else -> irCall(call, getOrCreateStaticMethod(function)).apply {
                            (0 until call.valueArgumentsCount).forEach {
                                putValueArgument(it, call.getValueArgument(it)!!)
                            }
                        }
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

    private fun createStaticBodilessMethod(function: IrFunction): IrSimpleFunction {
        val descriptor = WrappedSimpleFunctionDescriptor()
        val returnType = when (function) {
            is IrSimpleFunction -> function.returnType
            is IrConstructor -> function.parentAsClass.defaultType
            else -> error("Unknown function type")
        }

        return IrFunctionImpl(
            function.startOffset,
            function.endOffset,
            function.origin,
            IrSimpleFunctionSymbolImpl(descriptor),
            function.name.toInlineClassImplementationName(),
            function.visibility,
            Modality.FINAL,
            returnType,
            function.isInline,
            function.isExternal,
            (function is IrSimpleFunction && function.isTailrec),
            (function is IrSimpleFunction && function.isSuspend)
        ).apply {
            descriptor.bind(this)
            copyTypeParametersFrom(function)
            annotations += function.annotations
            dispatchReceiverParameter = null
            extensionReceiverParameter = function.extensionReceiverParameter?.copyTo(this)
            if (function is IrSimpleFunction) {
                valueParameters.add(function.dispatchReceiverParameter!!.copyTo(this, shift = 1))
                valueParameters += function.valueParameters.map { p -> p.copyTo(this, shift = 1) }
            } else {
                valueParameters += function.valueParameters.map { p -> p.copyTo(this, shift = 0) }
            }
            parent = function.parent
            assert(isStaticMethodOfClass)
        }
    }
}
