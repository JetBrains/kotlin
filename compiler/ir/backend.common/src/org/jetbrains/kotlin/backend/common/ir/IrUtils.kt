/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.CompilerVersionOfApiDeprecation
import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.lower.UpgradeCallableReferences
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.declarations.IrValueParameterBuilder
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irRichFunctionReference
import org.jetbrains.kotlin.ir.builders.setSourceRange
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.copyParametersFrom
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.StandardClassIds

fun IrReturnTarget.returnType(context: CommonBackendContext) =
    when (this) {
        is IrConstructor -> context.irBuiltIns.unitType
        is IrFunction -> returnType
        is IrReturnableBlock -> type
        else -> error("Unknown ReturnTarget: $this")
    }

@DeprecatedForRemovalCompilerApi(
    deprecatedSince = CompilerVersionOfApiDeprecation._2_1_20,
    replaceWith = "org.jetbrains.kotlin.ir.builders.declarations.buildReceiverParameter",
)
inline fun IrSimpleFunction.addDispatchReceiver(builder: IrValueParameterBuilder.() -> Unit): IrValueParameter =
    IrValueParameterBuilder().run {
        builder()
        name = "this".synthesizedName
        factory.buildValueParameter(this, this@addDispatchReceiver).also { receiver ->
            dispatchReceiverParameter = receiver
        }
    }

@DeprecatedForRemovalCompilerApi(
    deprecatedSince = CompilerVersionOfApiDeprecation._2_1_20,
    replaceWith = "org.jetbrains.kotlin.backend.common.ir.createExtensionReceiver",
)
fun IrSimpleFunction.addExtensionReceiver(type: IrType, origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED): IrValueParameter =
    IrValueParameterBuilder().run {
        this.type = type
        this.origin = origin
        this.name = "receiver".synthesizedName
        factory.buildValueParameter(this, this@addExtensionReceiver).also { receiver ->
            extensionReceiverParameter = receiver
        }
    }

fun IrSimpleFunction.createExtensionReceiver(type: IrType, origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED): IrValueParameter =
    IrValueParameterBuilder().run {
        this.type = type
        this.origin = origin
        this.name = "receiver".synthesizedName
        this.kind = IrParameterKind.ExtensionReceiver
        factory.buildValueParameter(this, this@createExtensionReceiver)
    }

// TODO: support more cases like built-in operator call and so on
fun IrExpression?.isPure(
    anyVariable: Boolean,
    checkFields: Boolean = true,
    symbols: Symbols? = null
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
                        ) && argument.isPure(anyVariable, checkFields, symbols)
            is IrCall -> symbols?.isSideEffectFree(this) == true && arguments.all { it.isPure(anyVariable, checkFields, symbols) }
            is IrGetObjectValue -> type.isUnit()
            is IrVararg -> elements.all { (it as? IrExpression)?.isPure(anyVariable, checkFields, symbols) == true }
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

    val arrayType = symbols.array.typeWith(arrayElementType)
    val arg0 = IrVarargImpl(startOffset, endOffset, arrayType, arrayElementType, arrayElements)

    return IrCallImpl(
        startOffset,
        endOffset,
        arrayType,
        symbols.arrayOf,
        typeArgumentsCount = 1,
    ).apply {
        typeArguments[0] = arrayElementType
        arguments[0] = arg0
    }
}

fun IrFunction.isInlineFunWithReifiedParameter() = isInline && typeParameters.any { it.isReified }

fun IrBranch.isUnconditional(): Boolean = (condition as? IrConst)?.value == true

fun syntheticBodyIsNotSupported(declaration: IrDeclaration): Nothing =
    compilationException("${IrSyntheticBody::class.java.simpleName} is not supported here", declaration)

val IrFile.isJvmBuiltin: Boolean get() = hasAnnotation(StandardClassIds.Annotations.JvmBuiltin)

val IrFile.isBytecodeGenerationSuppressed: Boolean get() = hasAnnotation(StandardClassIds.Annotations.SuppressBytecodeGeneration)

fun IrFunction.wrapWithLambdaCall(parent: IrDeclarationParent, context: CommonBackendContext): IrRichFunctionReference {
    require(this.typeParameters.isEmpty())
    val wrapper = factory.buildFun {
        setSourceRange(this@wrapWithLambdaCall)
        name = this@wrapWithLambdaCall.name
        visibility = DescriptorVisibilities.LOCAL
        returnType = this@wrapWithLambdaCall.returnType
    }.apply {
        this.parent = parent
        copyParametersFrom(this@wrapWithLambdaCall)
        val builder = context.createIrBuilder(this@apply.symbol).at(this@wrapWithLambdaCall)
        body = builder.irBlockBody {
            +irReturn(irCall(this@wrapWithLambdaCall).apply {
                for ((index, param) in parameters.withIndex()) {
                    arguments[index] = irGet(param)
                }
            })
        }
    }
    val builder = context.createIrBuilder(symbol).at(this@wrapWithLambdaCall)
    val referenceType = context.irBuiltIns.functionN(parameters.size).typeWith(parameters.map { it.type } + this@wrapWithLambdaCall.returnType)
    return builder.irRichFunctionReference(
        superType = referenceType,
        reflectionTargetSymbol = symbol,
        overriddenFunctionSymbol = UpgradeCallableReferences.selectSAMOverriddenFunction(referenceType),
        invokeFunction = wrapper,
        captures = emptyList(),
        origin = IrStatementOrigin.LAMBDA,
    )
}