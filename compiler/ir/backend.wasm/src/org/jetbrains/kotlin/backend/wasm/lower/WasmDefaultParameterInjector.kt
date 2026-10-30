/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.lower.DefaultParameterInjector
import org.jetbrains.kotlin.backend.common.lower.InnerClassesLowering
import org.jetbrains.kotlin.backend.common.lower.MaskedDefaultArgumentFunctionFactory
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.utils.getMostDeepValueClassUnderlyingType
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReinterpretCast
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultValueForType
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.util.OperatorNameConventions

class WasmMaskedDefaultArgumentFunctionFactory(
    private val ctx: WasmBackendContext,
    copyOriginalFunctionLocation: Boolean = true
) : MaskedDefaultArgumentFunctionFactory(ctx, copyOriginalFunctionLocation) {
    override fun IrType.hasNullAsUndefinedValue() =
        getMostDeepValueClassUnderlyingType(ctx) !in context.irBuiltIns.primitiveIrTypes
}

@PhasePrerequisites(InnerClassesLowering::class)
class WasmDefaultParameterInjector(
    context: WasmBackendContext
) : DefaultParameterInjector<WasmBackendContext>(context, WasmMaskedDefaultArgumentFunctionFactory(context), skipExternalMethods = true) {
    private val intOrSymbol by lazy { context.irBuiltIns.intClass.getSimpleFunction(OperatorNameConventions.OR.asString())!! }
    private val isUndefined = context.wasmSymbols.jsRelatedSymbols.jsIsUndefined
    private val intType = context.irBuiltIns.intType
    private lateinit var container: IrDeclaration

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        this.container = container
        super.lower(irBody, container)
    }

    override fun nullConst(startOffset: Int, endOffset: Int, type: IrType): IrExpression {
        val underlyingType = type.getMostDeepValueClassUnderlyingType(context)
        val underlyingValue = IrConstImpl.defaultValueForType(startOffset, endOffset, underlyingType)

        if (type == underlyingType) return underlyingValue

        return context.createIrBuilder(container.symbol)
            .irReinterpretCast(underlyingValue, type)
    }

    override fun shouldReplaceWithSyntheticFunction(functionAccess: IrFunctionAccessExpression): Boolean =
        functionAccess.origin == JS_EXPORT_ADAPTER_CALL || super.shouldReplaceWithSyntheticFunction(functionAccess)

    override fun argumentsForCall(
        builder: IrBlockBuilder,
        expression: IrFunctionAccessExpression,
        stubFunction: IrFunction,
    ): Map<IrValueParameter, IrExpression?> {
        if (expression.origin != JS_EXPORT_ADAPTER_CALL) {
            return super.argumentsForCall(builder, expression, stubFunction)
        }

        val result = mutableMapOf<IrValueParameter, IrExpression?>()
        expression.arguments.forEachIndexed { index, expression ->
            result[stubFunction.parameters[index]] = expression
        }

        val container = container as IrFunction
        val masks = stubFunction.parameters.filter { it.origin == IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION }
        val defaultableParameters = expression.symbol.owner.parameters.filter { it.canHaveDefaultValue() }
        with(context.createIrBuilder(container.symbol)) {
            for ([mask, parameters] in masks zip defaultableParameters.chunked(32)) {
                val maskValue = parameters.foldIndexed<IrValueParameter, IrExpression?>(null) { bit, value, parameter ->
                    if (parameter.defaultValue == null) return@foldIndexed value
                    val isOmitted = irCall(isUndefined).also {
                        it.arguments[0] = irGet(container.parameters[parameter.indexInParameters])
                    }

                    val bitIfOmitted = irIfThenElse(
                        type = intType,
                        condition = isOmitted,
                        thenPart = irInt(1 shl bit),
                        elsePart = irInt(0)
                    )

                    value?.let {
                        irCall(intOrSymbol).also {
                            it.arguments[0] = it
                            it.arguments[1] = bitIfOmitted
                        }
                    } ?: bitIfOmitted
                }
                result[mask] = maskValue ?: irInt(0)
            }
        }

        val handler = stubFunction.parameters.single { it.origin == IrDeclarationOrigin.METHOD_HANDLER_IN_DEFAULT_FUNCTION }
        result[handler] = nullConst(UNDEFINED_OFFSET, UNDEFINED_OFFSET, handler)

        return result
    }
}
