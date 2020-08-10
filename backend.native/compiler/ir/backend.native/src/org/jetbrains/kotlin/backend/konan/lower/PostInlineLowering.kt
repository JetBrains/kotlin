/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.error
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.isTypeOfIntrinsic
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * This pass runs after inlining and performs the following additional transformations over some operations:
 *     - Convert immutableBlobOf() arguments to special IrConst.
 *     - Convert `obj::class` and `Class::class` to calls.
 */
internal class PostInlineLowering(val context: Context) : FileLoweringPass {

    private val symbols get() = context.ir.symbols

    private val kTypeGenerator = KTypeGenerator(
            context,
            eraseTypeParameters = true // Mimic JVM BE behaviour until proper type parameter impl is ready.
    )

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            override fun visitClassReference(expression: IrClassReference): IrExpression {
                expression.transformChildrenVoid()

                val builder = createIrBuilder(expression)

                val symbol = expression.symbol
                return if (symbol is IrClassSymbol) {
                    builder.irKClass(context, symbol)
                } else {
                    // E.g. for `T::class` in a body of an inline function itself.
                    builder.irCall(context.ir.symbols.throwNullPointerException.owner)
                }
            }

            override fun visitGetClass(expression: IrGetClass): IrExpression {
                expression.transformChildrenVoid()

                val builder = createIrBuilder(expression)

                val typeArgument = expression.argument.type

                return builder.irCall(symbols.kClassImplConstructor, listOf(typeArgument)).apply {
                    val typeInfo = builder.irCall(symbols.getObjectTypeInfo).apply {
                        putValueArgument(0, expression.argument)
                    }

                    putValueArgument(0, typeInfo)
                }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                // Function inlining is changing function symbol at callsite
                // and unbound symbol replacement is happening later.
                // So we compare descriptors for now.
                if (expression.symbol == symbols.immutableBlobOf) {
                    // Convert arguments of the binary blob to special IrConst<String> structure, so that
                    // vararg lowering will not affect it.
                    val args = expression.getValueArgument(0) as? IrVararg
                            ?: throw Error("varargs shall not be lowered yet")
                    if (args.elements.any { it is IrSpreadElement }) {
                        context.reportCompilationError("no spread elements allowed here", irFile, args)
                    }
                    val builder = StringBuilder()
                    args.elements.forEach {
                        if (it !is IrConst<*>) {
                            context.reportCompilationError(
                                    "all elements of binary blob must be constants", irFile, it)
                        }
                        val value = when (it.kind) {
                            IrConstKind.Short ->  (it.value as Short).toInt()
                            else ->
                                context.reportCompilationError("incorrect value for binary data: $it.value", irFile, it)
                        }
                        if (value < 0 || value > 0xff)
                            context.reportCompilationError("incorrect value for binary data: $value", irFile, it)
                        // Luckily, all values in range 0x00 .. 0xff represent valid UTF-16 symbols,
                        // block 0 (Basic Latin) and block 1 (Latin-1 Supplement) in
                        // Basic Multilingual Plane, so we could just append data "as is".
                        builder.append(value.toChar())
                    }
                    expression.putValueArgument(0, IrConstImpl(
                            expression.startOffset, expression.endOffset,
                            context.irBuiltIns.stringType,
                            IrConstKind.String, builder.toString()))
                } else if (expression.symbol.owner.isTypeOfIntrinsic()) {
                    val type = expression.getTypeArgument(0)
                            ?: error(irFile, expression, "missing type argument")
                    return with (kTypeGenerator) { createIrBuilder(expression).irKType(type) }
                }

                return expression
            }

            private fun createIrBuilder(element: IrElement) = context.createIrBuilder(
                    currentScope!!.scope.scopeOwnerSymbol,
                    element.startOffset,
                    element.endOffset
            )
        })
    }
}