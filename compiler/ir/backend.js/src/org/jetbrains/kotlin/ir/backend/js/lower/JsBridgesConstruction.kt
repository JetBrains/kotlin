/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrArithBuilder
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.varargParameterIndex
import org.jetbrains.kotlin.ir.backend.js.utils.eraseGenerics
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.backend.js.utils.hasStableJsName
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name

class JsBridgesConstruction(context: JsIrBackendContext) : BridgesConstruction<JsIrBackendContext>(context) {

    private val calculator = JsIrArithBuilder(context)

    private val jsArguments = context.intrinsics.jsArguments
    private val jsArrayGet = context.intrinsics.jsArrayGet
    private val jsArrayLength = context.intrinsics.jsArrayLength
    private val jsArrayLike2Array = context.intrinsics.jsArrayLike2Array
    private val jsSliceArrayLikeFromIndex = context.intrinsics.jsSliceArrayLikeFromIndex
    private val jsSliceArrayLikeFromIndexToIndex = context.intrinsics.jsSliceArrayLikeFromIndexToIndex
    private val primitiveArrays = context.intrinsics.primitiveArrays
    private val primitiveToLiteralConstructor = context.intrinsics.primitiveToLiteralConstructor

    private fun IrType.getJsInlinedClass() = context.inlineClassesUtils.getInlinedClass(this)

    override fun getFunctionSignature(function: IrSimpleFunction): JsSignature =
        if (function.hasStableJsName(context)) {
            JsStableNameSignature(function.getJsNameOrKotlinName())
        } else {
            JsNonStableSignature(
                function.name,
                function.extensionReceiverParameter?.type?.eraseGenerics(context.irBuiltIns),
                function.valueParameters.map { it.type.eraseGenerics(context.irBuiltIns) },
                function.returnType.takeIf {
                    it.getJsInlinedClass() != null || it.isUnit()
                }?.eraseGenerics(context.irBuiltIns)
            )
        }

    override fun getBridgeOrigin(bridge: IrSimpleFunction): IrDeclarationOrigin =
        when {
            bridge.hasStableJsName(context) -> JsLoweredDeclarationOrigin.BRIDGE_WITH_STABLE_NAME
            bridge.correspondingPropertySymbol != null -> JsLoweredDeclarationOrigin.BRIDGE_PROPERTY_ACCESSOR
            else -> JsLoweredDeclarationOrigin.BRIDGE_WITHOUT_STABLE_NAME
        }

    override fun extractValueParameters(
        blockBodyBuilder: IrBlockBodyBuilder,
        irFunction: IrSimpleFunction,
        bridge: IrSimpleFunction
    ): List<IrValueDeclaration> {

        if (!bridge.isEffectivelyExternal())
            return super.extractValueParameters(blockBodyBuilder, irFunction, bridge)

        val varargIndex = bridge.varargParameterIndex()

        if (varargIndex == -1)
            return super.extractValueParameters(blockBodyBuilder, irFunction, bridge)

        return blockBodyBuilder.run {

            // The number of parameters after the vararg
            val numberOfTrailingParameters = bridge.valueParameters.size - (varargIndex + 1)

            val getTotalNumberOfArguments = irCall(jsArrayLength).apply {
                putValueArgument(0, irCall(jsArguments))
                type = context.irBuiltIns.intType
            }

            val firstTrailingParameterIndexVar = lazy {
                irTemporary(
                    if (numberOfTrailingParameters == 0)
                        getTotalNumberOfArguments
                    else
                        calculator.sub(
                            getTotalNumberOfArguments,
                            irInt(numberOfTrailingParameters)
                        ),
                    nameHint = "firstTrailingParameterIndex"
                )
            }

            val varargArrayVar = emitCopyVarargToArray(
                bridge,
                varargIndex,
                numberOfTrailingParameters,
                firstTrailingParameterIndexVar
            )

            val trailingParametersVars = createVarsForTrailingParameters(bridge, numberOfTrailingParameters, firstTrailingParameterIndexVar)

            irFunction.valueParameters + varargArrayVar + trailingParametersVars
        }
    }

    private fun IrBlockBodyBuilder.createVarsForTrailingParameters(
        bridge: IrSimpleFunction,
        numberOfTrailingParameters: Int,
        firstTrailingParameterIndexVar: Lazy<IrVariable>
    ) = bridge.valueParameters.takeLast(numberOfTrailingParameters).mapIndexed { index, trailingParameter ->
        val parameterIndex = if (index == 0)
            irGet(firstTrailingParameterIndexVar.value)
        else
            calculator.add(irGet(firstTrailingParameterIndexVar.value), irInt(index))
        createTmpVariable(
            irCall(jsArrayGet).apply {
                putValueArgument(0, irCall(jsArguments))
                putValueArgument(1, parameterIndex)
            },
            nameHint = trailingParameter.name.asString(),
            irType = trailingParameter.type
        )
    }

    private fun IrBlockBodyBuilder.emitCopyVarargToArray(
        bridge: IrSimpleFunction,
        varargIndex: Int,
        numberOfTrailingParameters: Int,
        firstTrailingParameterIndexVar: Lazy<IrVariable>
    ): IrVariable {
        val varargElement = bridge.valueParameters[varargIndex]
        val sliceIntrinsicArgs = mutableListOf<IrExpression>(irCall(jsArguments))
        var sliceIntrinsic = jsArrayLike2Array
        if (varargIndex != 0 || numberOfTrailingParameters > 0) {
            sliceIntrinsicArgs.add(irInt(varargIndex))
            sliceIntrinsic = jsSliceArrayLikeFromIndex
        }
        if (numberOfTrailingParameters > 0) {
            sliceIntrinsicArgs.add(irGet(firstTrailingParameterIndexVar.value))
            sliceIntrinsic = jsSliceArrayLikeFromIndexToIndex
        }

        val varargCopiedAsArray = irCall(sliceIntrinsic).apply {
            putTypeArgument(0, varargElement.varargElementType!!)
            sliceIntrinsicArgs.forEachIndexed(this::putValueArgument)
        }.let { arrayExpr ->
            val arrayInfo =
                InlineClassArrayInfo(this@JsBridgesConstruction.context, varargElement.varargElementType!!, varargElement.type)
            val primitiveType = primitiveArrays[arrayInfo.primitiveArrayType.classifierOrNull]
            if (primitiveType != null) {
                arrayInfo.boxArrayIfNeeded(
                    irCall(primitiveToLiteralConstructor.getValue(primitiveType)).apply {
                        putValueArgument(0, arrayExpr)
                        type = varargElement.type
                    }
                )
            } else {
                arrayExpr
            }
        }

        return createTmpVariable(varargCopiedAsArray, nameHint = varargElement.name.asString())
    }
}

interface JsSignature {
    val name: Name
}

data class JsNonStableSignature(
    override val name: Name,
    val extensionReceiverType: IrType?,
    val valueParametersType: List<IrType>,
    val returnType: IrType?,
) : JsSignature {
    override fun toString(): String {
        val er = extensionReceiverType?.let { "(er: ${it.render()}) " } ?: ""
        val parameters = valueParametersType.joinToString(", ") { it.render() }
        return "[$er$name($parameters) -> ${returnType?.let { " -> ${it.render()}" } ?: ""}]"
    }
}

data class JsStableNameSignature(
    override val name: Name,
) : JsSignature
