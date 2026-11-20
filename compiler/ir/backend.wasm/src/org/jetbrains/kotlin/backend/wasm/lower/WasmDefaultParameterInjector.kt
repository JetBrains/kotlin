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
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.util.OperatorNameConventions

@PhasePrerequisites(InnerClassesLowering::class)
class WasmDefaultParameterInjector(
    context: WasmBackendContext
) : DefaultParameterInjector<WasmBackendContext>(context, MaskedDefaultArgumentFunctionFactory(context), skipExternalMethods = true) {
    private val intOrSymbol by lazy { context.irBuiltIns.intClass.getSimpleFunction(OperatorNameConventions.OR.asString())!! }
    private lateinit var container: IrDeclaration

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        this.container = container
        super.lower(irBody, container)
    }

    override fun shouldReplaceWithSyntheticFunction(functionAccess: IrFunctionAccessExpression): Boolean =
        functionAccess.origin == JS_EXPORT_ADAPTER_CALL || super.shouldReplaceWithSyntheticFunction(functionAccess)

    override fun visitCall(expression: IrCall): IrExpression {
        val stubCall = super.visitCall(expression)
        if (expression.origin != JS_EXPORT_ADAPTER_CALL || stubCall === expression) return stubCall

        // All the arguments are passed, so set the bits of the ones omitted in JS at runtime,
        // checking the parameters of the adapter, which correspond to the parameters of the function.
        stubCall as IrCall
        val adapter = container as IrFunction
        val isUndefined = context.wasmSymbols.jsRelatedSymbols.jsInteropAdapters.jsIsUndefinedAdapter
        val intType = context.irBuiltIns.intType
        val masks = stubCall.symbol.owner.parameters.filter { it.origin == IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION }
        val defaultableParameters = expression.symbol.owner.parameters.filter { it.canHaveDefaultValue() }
        with(context.createIrBuilder(adapter.symbol)) {
            for ([mask, parameters] in masks zip defaultableParameters.chunked(32)) {
                val maskValue = parameters.foldIndexed<IrValueParameter, IrExpression>(irInt(0)) { bit, value, parameter ->
                    if (parameter.defaultValue == null) return@foldIndexed value
                    val isOmitted = irCall(isUndefined).also { it.arguments[0] = irGet(adapter.parameters[parameter.indexInParameters]) }
                    irCall(intOrSymbol).also {
                        it.arguments[0] = value
                        it.arguments[1] = irIfThenElse(intType, isOmitted, irInt(1 shl bit), irInt(0))
                    }
                }
                stubCall.arguments[mask.indexInParameters] = maskValue
            }
        }
        return stubCall
    }
}
