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
import org.jetbrains.kotlin.backend.jvm.InlineClassAbi
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.JvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.representativeUpperBound
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal val jvmStringConcatenationLowering = makeIrFilePhase(
    { context: JvmBackendContext ->
        if (!context.config.runtimeStringConcat.isDynamic)
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

private fun JvmIrBuilder.callToString(expression: IrExpression): IrExpression {
    val argument = normalizeArgument(expression)
    val argumentType = if (argument.type.isPrimitiveType()) argument.type else context.irBuiltIns.anyNType

    return irCall(backendContext.ir.symbols.typeToStringValueOfFunction(argumentType)).apply {
        putValueArgument(0, argument)
    }
}

private fun JvmIrBuilder.normalizeArgument(expression: IrExpression): IrExpression {
    val type = expression.type
    if (type.isByte() || type.isShort()) {
        // There is no special append or valueOf function for byte and short on the JVM.
        return irImplicitCast(expression, context.irBuiltIns.intType)
    }

    if (expression is IrConst<*> && expression.kind == IrConstKind.String && (expression.value as String).length == 1) {
        // PSI2IR generates const Strings for 1-length literals in string templates (e.g., the space between x and y in "$x $y").
        // We want to use the more efficient `append(Char)` function in such cases. This mirrors the behavior of the non-IR backend.
        //
        // In addition, this also means `append(Char)` will be used for the space in the following case: `x + " " + y`.
        // The non-IR backend will still use `append(String)` in this case.
        // NB KT-50091 shows an outlier where this might be actually less efficient, but in general we prefer `Char`.
        return irChar((expression.value as String)[0])
    }

    val typeParameterSymbol = type.classifierOrNull as? IrTypeParameterSymbol
    if (typeParameterSymbol != null) {
        // Upcast type parameter to upper bound with specialized 'append' function
        val upperBound = typeParameterSymbol.owner.representativeUpperBound
        if (upperBound.classifierOrNull == context.irBuiltIns.stringClass) {
            //  T <: String || T <: String? =>
            //      upcast to 'String?'
            return irImplicitCast(expression, context.irBuiltIns.stringType.makeNullable())
        }
        if (!type.isNullable()) {
            if (upperBound.isByte() || upperBound.isShort()) {
                //  Expression type is not null,
                //  T <: Byte || T <: Short =>
                //      upcast to Int
                return irImplicitCast(expression, context.irBuiltIns.intType)
            } else if (upperBound.isPrimitiveType()) {
                //  Expression type is not null,
                //  T <: P, P is primitive type (other than 'Byte' or 'Short') =>
                //      upcast to P
                return irImplicitCast(expression, upperBound)
            }
        }
    }

    return expression
}


private fun JvmIrBuilder.lowerInlineClassArgument(expression: IrExpression): IrExpression? {
    if (InlineClassAbi.unboxType(expression.type) == null)
        return null
    val toStringFunction = expression.type.classOrNull?.owner?.toStringFunction
        ?.let { (it as? IrAttributeContainer)?.attributeOwnerId as? IrFunction ?: it }
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

private const val MAX_STRING_CONCAT_DEPTH = 23

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
        it.valueParameters.isEmpty()
    }

    private val toStringFunction = stringBuilder.toStringFunction

    private val appendAnyNFunction =
        findStringBuilderAppendFunctionWithParameter { it.type.isNullableAny() }!!

    private val appendStringNFunction =
        findStringBuilderAppendFunctionWithParameter { it.type.classOrNull == context.irBuiltIns.stringClass }!!

    private val appendFunctionsByParameterType: Map<IrType, IrSimpleFunction> =
        stringBuilder.functions
            .filter { it.isAppendFunction() }
            .associateBy { it.valueParameters[0].type }

    private inline fun findStringBuilderAppendFunctionWithParameter(predicate: (IrValueParameter) -> Boolean) =
        stringBuilder.functions.find {
            it.isAppendFunction() && predicate(it.valueParameters[0])
        }

    private fun IrSimpleFunction.isAppendFunction() =
        name.asString() == "append" && valueParameters.size == 1

    private fun typeToAppendFunction(type: IrType): IrSimpleFunction {
        appendFunctionsByParameterType[type]?.let { return it }

        if (type.classOrNull == context.irBuiltIns.stringClass)
            return appendStringNFunction

        return appendAnyNFunction
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        expression.transformChildrenVoid(this)
        return context.createJvmIrBuilder(currentScope!!, expression).run {
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

                arguments.size < MAX_STRING_CONCAT_DEPTH -> {
                    irCall(toStringFunction).apply {
                        dispatchReceiver = appendWindow(arguments, irCall(constructor))
                    }
                }

                else -> {
                    // arguments.size >= MAX_STRING_CONCAT_DEPTH. Prevent SOE in ExpressionCodegen.
                    // Generates:
                    //  {
                    //      val tmp = StringBuilder()
                    //      tmp.append(a0).append(a1) /* ... */ .append(a21).append(a22)
                    //      /* ... repeat for each possibly partial MAX_STRING_CONCAT_DEPTH-element window ... */
                    //      tmp.toString()
                    //  }
                    irBlock {
                        val tmpStringBuilder = irTemporary(irCall(constructor))
                        val argsWindowed =
                            arguments.windowed(
                                size = MAX_STRING_CONCAT_DEPTH,
                                step = MAX_STRING_CONCAT_DEPTH,
                                partialWindows = true
                            )
                        for (argsWindow in argsWindowed) {
                            +appendWindow(argsWindow, irGet(tmpStringBuilder))
                        }
                        +irCall(toStringFunction).apply { dispatchReceiver = irGet(tmpStringBuilder) }
                    }
                }
            }
        }
    }

    private fun JvmIrBuilder.appendWindow(arguments: List<IrExpression>, stringBuilder0: IrExpression): IrExpression {
        return arguments.fold(stringBuilder0) { stringBuilder, arg ->
            val argument = normalizeArgument(arg)
            val appendFunction = typeToAppendFunction(argument.type)
            irCall(appendFunction).apply {
                dispatchReceiver = stringBuilder
                // Unwrapping IMPLICIT_NOTNULL is necessary for ALL arguments. There could be a call to `String.plus(Any?)`
                // anywhere in the flattened IrStringConcatenation expression, e.g., `"foo" + (Java.platformString() + 123)`.
                putValueArgument(0, lowerInlineClassArgument(argument) ?: argument.unwrapImplicitNotNull())
            }
        }
    }
}

/**
 * This lowering pass lowers inline classes arguments of [IrStringConcatenation].
 * Transformed [IrStringConcatenation] would be used as is in [ExpressionCodegen] for makeConcat/makeConcatWithConstants bytecode generation
 */
private class JvmDynamicStringConcatenationLowering(val context: JvmBackendContext) : FileLoweringPass,
    IrElementTransformerVoidWithContext() {
    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        expression.transformChildrenVoid(this)
        return context.createJvmIrBuilder(currentScope!!, expression).run {
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

