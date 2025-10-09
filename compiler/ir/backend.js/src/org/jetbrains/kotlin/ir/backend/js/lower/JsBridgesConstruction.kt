/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrArithBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.hasStableJsName
import org.jetbrains.kotlin.ir.backend.js.utils.jsFunctionSignature
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

class JsBridgesConstruction(val context: JsIrBackendContext) : BridgesConstruction(context) {

    private val calculator = JsIrArithBuilder(context)

    private val jsArguments = context.symbols.jsArguments
    private val jsArrayGet = context.symbols.jsArrayGet
    private val jsArrayLength = context.symbols.jsArrayLength
    private val jsArrayLike2Array = context.symbols.jsArrayLike2Array
    private val jsSliceArrayLikeFromIndex = context.symbols.jsSliceArrayLikeFromIndex
    private val jsSliceArrayLikeFromIndexToIndex = context.symbols.jsSliceArrayLikeFromIndexToIndex
    private val primitiveArrays = context.irBuiltIns.primitiveArraysToPrimitiveTypes
    private val primitiveToLiteralConstructor = context.symbols.primitiveToLiteralConstructor

    override fun getFunctionSignature(function: IrSimpleFunction) =
        jsFunctionSignature(
            function,
            context
        )

    override fun findConcreteSuperDeclaration(function: IrSimpleFunction): IrSimpleFunction =
        if (function.isRealOrOverridesInterface) function else function.realOverrideTarget

    override fun getBridgeOrigin(bridge: IrSimpleFunction): IrDeclarationOrigin =
        when {
            bridge.hasStableJsName(context) -> JsLoweredDeclarationOrigin.BRIDGE_WITH_STABLE_NAME
            bridge.correspondingPropertySymbol != null -> JsLoweredDeclarationOrigin.BRIDGE_PROPERTY_ACCESSOR
            else -> JsLoweredDeclarationOrigin.BRIDGE_WITHOUT_STABLE_NAME
        }

    override fun extractParameters(
        blockBodyBuilder: IrBlockBodyBuilder,
        irFunction: IrSimpleFunction,
        bridge: IrSimpleFunction
    ): List<IrValueDeclaration> {

        if (!bridge.isEffectivelyExternal())
            return super.extractParameters(blockBodyBuilder, irFunction, bridge)

        val varargIndex = bridge.parameters.indexOfFirst { it.isVararg }

        if (varargIndex == -1)
            return super.extractParameters(blockBodyBuilder, irFunction, bridge)

        return blockBodyBuilder.run {

            // The number of parameters after the vararg
            val numberOfTrailingParameters = bridge.parameters.size - (varargIndex + 1)

            val getTotalNumberOfArguments = irCall(jsArrayLength).apply {
                arguments[0] = irCall(jsArguments)
                type = context.irBuiltIns.intType
            }

            val firstTrailingParameterIndexVar = lazy(LazyThreadSafetyMode.NONE) {
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

            irFunction.parameters + varargArrayVar + trailingParametersVars
        }
    }

    private fun IrBlockBodyBuilder.createVarsForTrailingParameters(
        bridge: IrSimpleFunction,
        numberOfTrailingParameters: Int,
        firstTrailingParameterIndexVar: Lazy<IrVariable>
    ) = bridge.parameters.takeLast(numberOfTrailingParameters).mapIndexed { index, trailingParameter ->
        val parameterIndex = if (index == 0)
            irGet(firstTrailingParameterIndexVar.value)
        else
            calculator.add(irGet(firstTrailingParameterIndexVar.value), irInt(index))
        createTmpVariable(
            irCall(jsArrayGet).apply {
                arguments[0] = irCall(jsArguments)
                arguments[1] = parameterIndex
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
        val varargElement = bridge.parameters[varargIndex]
        val sliceIntrinsicArgs = mutableListOf<IrExpression>(irCall(jsArguments))
        var sliceIntrinsic = jsArrayLike2Array
        if (varargIndex != 0 || numberOfTrailingParameters > 0) {
            val nonDispatchVarargIndex = varargIndex - if (bridge.dispatchReceiverParameter != null) 1 else 0
            sliceIntrinsicArgs.add(irInt(nonDispatchVarargIndex))
            sliceIntrinsic = jsSliceArrayLikeFromIndex
        }
        if (numberOfTrailingParameters > 0) {
            sliceIntrinsicArgs.add(irGet(firstTrailingParameterIndexVar.value))
            sliceIntrinsic = jsSliceArrayLikeFromIndexToIndex
        }

        val varargCopiedAsArray = irCall(sliceIntrinsic).apply {
            typeArguments[0] = varargElement.varargElementType!!
            arguments.assignFrom(sliceIntrinsicArgs)
        }.let { arrayExpr ->
            val arrayInfo =
                InlineClassArrayInfo(this@JsBridgesConstruction.context, varargElement.varargElementType!!, varargElement.type)
            val primitiveType = primitiveArrays[arrayInfo.primitiveArrayType.classifierOrNull]
            if (primitiveType != null) {
                arrayInfo.boxArrayIfNeeded(
                    irCall(primitiveToLiteralConstructor.getValue(primitiveType)).apply {
                        arguments[0] = arrayExpr
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
