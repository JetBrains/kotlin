/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.FlattenStringConcatenationLowering
import org.jetbrains.kotlin.backend.common.lower.flattenStringConcatenationPhase
import org.jetbrains.kotlin.backend.common.lower.loops.forLoopsPhase
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.JvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.InlineClassAbi
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.unboxInlineClass
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal val jvmStringConcatenationLowering = makeIrFilePhase<JvmBackendContext>(
    { context: JvmBackendContext ->
        if (!context.state.runtimeStringConcat.isDynamic)
            JvmStringConcatenationLowering(context)
        else
            JvmDynamicStringConcatenationLowering(context)
    },
    name = "StringConcatenation",
    description = "Replace IrStringConcatenation with string builders",
    // flattenStringConcatenationPhase consolidates string concatenation expressions.
    // forLoopsPhase may produce IrStringConcatenations.
    prerequisite = setOf(flattenStringConcatenationPhase, forLoopsPhase)
)

private val IrClass.toStringFunction: IrSimpleFunction
    get() = functions.single {
        with(FlattenStringConcatenationLowering) { it.isToString }
    }


private fun IrBuilderWithScope.normalizeArgument(expression: IrExpression): IrExpression =
    if (expression.type.isByte() || expression.type.isShort()) {
        // There is no special append or valueOf function for byte and short on the JVM.
        irImplicitCast(expression, context.irBuiltIns.intType)
    } else if (expression is IrConst<*> && expression.kind == IrConstKind.String && (expression.value as String).length == 1) {
        // PSI2IR generates const Strings for 1-length literals in string templates (e.g., the space between x and y in "$x $y").
        // We want to use the more efficient `append(Char)` function in such cases. This mirrors the behavior of the non-IR backend.
        //
        // In addition, this also means `append(Char)` will be used for the space in the following case: `x + " " + y`. The non-IR
        // backend will still use `append(String)` in this case.
        irChar((expression.value as String)[0])
    } else {
        expression
    }

private fun JvmIrBuilder.callToString(expression: IrExpression): IrExpression {
    val argument = normalizeArgument(expression)
    val argumentType = if (argument.type.isPrimitiveType()) argument.type else context.irBuiltIns.anyNType

    return irCall(backendContext.ir.symbols.typeToStringValueOfFunction(argumentType)).apply {
        putValueArgument(0, argument)
    }
}

private fun JvmIrBuilder.lowerInlineClassArgument(expression: IrExpression): IrExpression? {
    if (InlineClassAbi.unboxType(expression.type) == null)
        return null
    val toStringFunction = expression.type.classOrNull?.owner?.toStringFunction
        ?: return null
    val toStringReplacement = backendContext.inlineClassReplacements.getReplacementFunction(toStringFunction)
        ?: return null
    // `C?` can only be unboxed if it wraps a reference type `T!!`, in which case the unboxed type
    // is `T?`. We can't pass that to `C.toString-impl` without checking for `null`.
    return if (expression.type.isNullable())
        irLetS(expression) {
            irIfNull(context.irBuiltIns.stringType, irGet(it.owner), irString(null.toString()), irCall(toStringReplacement).apply {
                putValueArgument(0, irGet(it.owner))
            })
        }
    else
        irCall(toStringReplacement).apply { putValueArgument(0, expression) }
}

private fun IrExpression.unwrapImplicitNotNull() =
    if (this is IrTypeOperatorCall && operator == IrTypeOperator.IMPLICIT_NOTNULL)
        argument
    else
        this

/**
 * This lowering pass replaces [IrStringConcatenation]s with StringBuilder appends.
 *
 * This pass is based on [StringConcatenationLowering] in backend.common. The main difference
 * is that this pass also handles JVM specific optimizations, such as calling stringPlus
 * for two arguments, and properly handles inline classes.
 */
private class JvmStringConcatenationLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    private val stringBuilder = context.ir.symbols.stringBuilder.owner

    private val constructor = stringBuilder.constructors.single {
        it.valueParameters.size == 0
    }

    private val toStringFunction = stringBuilder.toStringFunction

    private val defaultAppendFunction = stringBuilder.functions.single {
        it.name.asString() == "append" &&
                it.valueParameters.size == 1 &&
                it.valueParameters.single().type.isNullableAny()
    }

    private val appendFunctions: Map<IrType, IrSimpleFunction?> =
        (context.irBuiltIns.primitiveIrTypes + context.irBuiltIns.stringType).associateWith { type ->
            stringBuilder.functions.singleOrNull {
                it.name.asString() == "append" && it.valueParameters.singleOrNull()?.type == type
            }
        }

    private fun typeToAppendFunction(type: IrType): IrSimpleFunction =
        appendFunctions[type] ?: defaultAppendFunction

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        expression.transformChildrenVoid(this)
        return context.createJvmIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).run {
            // When `String.plus(Any?)` is invoked with receiver of platform type String or String with enhanced nullability, this could
            // fail a nullability check (NullPointerException) on the receiver. However, the non-IR backend currently does NOT insert this
            // check (see KT-36625, pending language design decision). To maintain compatibility with the non-IR backend, we remove
            // IMPLICIT_NOTNULL casts from all arguments (nullability checks are generated in JvmArgumentNullabilityAssertionsLowering).

            val arguments = expression.arguments
            when {
                arguments.isEmpty() ->
                    irString("")

                arguments.size == 1 ->
                    lowerInlineClassArgument(arguments[0]) ?: callToString(arguments[0].unwrapImplicitNotNull())

                arguments.size == 2 && arguments[0].type.isStringClassType() ->
                    irCall(backendContext.ir.symbols.intrinsicStringPlus).apply {
                        putValueArgument(0, lowerInlineClassArgument(arguments[0]) ?: arguments[0].unwrapImplicitNotNull())
                        // Unwrapping IMPLICIT_NOTNULL is not strictly necessary on 2nd argument (parameter type is `Any?`)
                        putValueArgument(1, lowerInlineClassArgument(arguments[1]) ?: arguments[1])
                    }

                else -> {
                    var stringBuilder = irCall(constructor)
                    for (arg in arguments) {
                        val argument = normalizeArgument(arg)
                        val appendFunction = typeToAppendFunction(argument.type)
                        stringBuilder = irCall(appendFunction).apply {
                            dispatchReceiver = stringBuilder
                            // Unwrapping IMPLICIT_NOTNULL is necessary for ALL arguments. There could be a call to `String.plus(Any?)`
                            // anywhere in the flattened IrStringConcatenation expression, e.g., `"foo" + (Java.platformString() + 123)`.
                            putValueArgument(0, lowerInlineClassArgument(argument) ?: argument.unwrapImplicitNotNull())
                        }
                    }
                    irCall(toStringFunction).apply {
                        dispatchReceiver = stringBuilder
                    }
                }
            }
        }
    }
}

/**
 * This lowering pass lowers inline classes arguments of [IrStringConcatenation].
 * Transformed [IrStringConcatenation] would be used as is in [ExpressionCodegen] for makeConcat/makeConcatWithConstants bytecode generation
 */
private class JvmDynamicStringConcatenationLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        expression.transformChildrenVoid(this)
        return context.createJvmIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).run {
            // When `String.plus(Any?)` is invoked with receiver of platform type String or String with enhanced nullability, this could
            // fail a nullability check (NullPointerException) on the receiver. However, the non-IR backend currently does NOT insert this
            // check (see KT-36625, pending language design decision). To maintain compatibility with the non-IR backend, we remove
            // IMPLICIT_NOTNULL casts from all arguments (nullability checks are generated in JvmArgumentNullabilityAssertionsLowering).

            val arguments = expression.arguments
            when {
                arguments.isEmpty() ->
                    irString("")

                else -> {
                    IrStringConcatenationImpl(
                        expression.startOffset,
                        expression.endOffset,
                        expression.type,
                        arguments.map { argument ->
                            lowerInlineClassArgument(argument) ?: argument.unwrapImplicitNotNull()
                        })
                }
            }
        }
    }
}

