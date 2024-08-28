/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.ir.builders.declarations.IrValueParameterBuilder
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.StandardClassIds

fun IrReturnTarget.returnType(context: CommonBackendContext) =
    when (this) {
        is IrConstructor -> context.irBuiltIns.unitType
        is IrFunction -> returnType
        is IrReturnableBlock -> type
        else -> error("Unknown ReturnTarget: $this")
    }

inline fun IrSimpleFunction.addDispatchReceiver(builder: IrValueParameterBuilder.() -> Unit): IrValueParameter =
    IrValueParameterBuilder().run {
        builder()
        name = "this".synthesizedName
        factory.buildValueParameter(this, this@addDispatchReceiver).also { receiver ->
            dispatchReceiverParameter = receiver
        }
    }

fun IrSimpleFunction.addExtensionReceiver(type: IrType, origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED): IrValueParameter =
    IrValueParameterBuilder().run {
        this.type = type
        this.origin = origin
        this.name = "receiver".synthesizedName
        factory.buildValueParameter(this, this@addExtensionReceiver).also { receiver ->
            extensionReceiverParameter = receiver
        }
    }

// TODO: support more cases like built-in operator call and so on
fun IrExpression?.isPure(
    anyVariable: Boolean,
    checkFields: Boolean = true,
    context: CommonBackendContext? = null
): Boolean {
    if (this == null) return true

    fun IrExpression.isPureImpl(): Boolean {
        return when (this) {
            is IrConst -> true
            is IrGetValue -> {
                if (anyVariable) return true
                val valueDeclaration = symbol.owner
                if (valueDeclaration is IrVariable) !valueDeclaration.isVar
                else true
            }
            is IrTypeOperatorCall ->
                (
                        operator == IrTypeOperator.INSTANCEOF ||
                                operator == IrTypeOperator.REINTERPRET_CAST ||
                                operator == IrTypeOperator.NOT_INSTANCEOF
                        ) && argument.isPure(anyVariable, checkFields, context)
            is IrCall -> if (context?.isSideEffectFree(this) == true) {
                for (i in 0 until valueArgumentsCount) {
                    val valueArgument = getValueArgument(i)
                    if (!valueArgument.isPure(anyVariable, checkFields, context)) return false
                }
                true
            } else false
            is IrGetObjectValue -> type.isUnit()
            is IrVararg -> elements.all { (it as? IrExpression)?.isPure(anyVariable, checkFields, context) == true }
            else -> false
        }
    }

    if (isPureImpl()) return true

    if (!checkFields) return false

    if (this is IrGetField) {
        if (!symbol.owner.isFinal) {
            if (!anyVariable) {
                return false
            }
        }
        return receiver.isPure(anyVariable)
    }

    return false
}

fun CommonBackendContext.createArrayOfExpression(
    startOffset: Int, endOffset: Int,
    arrayElementType: IrType,
    arrayElements: List<IrExpression>
): IrExpression {

    val arrayType = ir.symbols.array.typeWith(arrayElementType)
    val arg0 = IrVarargImpl(startOffset, endOffset, arrayType, arrayElementType, arrayElements)

    return IrCallImpl(
        startOffset,
        endOffset,
        arrayType,
        ir.symbols.arrayOf,
        typeArgumentsCount = 1,
    ).apply {
        putTypeArgument(0, arrayElementType)
        putValueArgument(0, arg0)
    }
}

fun IrFunction.isInlineFunWithReifiedParameter() = isInline && typeParameters.any { it.isReified }

fun IrBranch.isUnconditional(): Boolean = (condition as? IrConst)?.value == true

fun syntheticBodyIsNotSupported(declaration: IrDeclaration): Nothing =
    compilationException("${IrSyntheticBody::class.java.simpleName} is not supported here", declaration)

val IrFile.producesMetadata: Boolean get() = hasAnnotation(StandardClassIds.Annotations.ProducesBuiltinMetadata)

val IrFile.isBuiltin: Boolean get() = hasAnnotation(StandardClassIds.Annotations.Builtin)