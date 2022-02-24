/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.getOrPut
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irReturnUnit
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Replaces suspend functions with regular non-suspend functions with additional
 * continuation parameter `$cont` of type [kotlin.coroutines.Continuation].
 *
 * Replaces return type with `Any?` or `Any` (for non-nullable types) to indicate that suspend
 * functions can return special values like [kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED]
 * which might not be a subtype of original return type.
 */
class AddContinuationToNonLocalSuspendFunctionsLowering(val context: JsCommonBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? =
        if (declaration is IrSimpleFunction && declaration.isSuspend) {
            listOf(transformSuspendFunction(context, declaration))
        } else {
            null
        }
}

/**
 * Similar to [AddContinuationToNonLocalSuspendFunctionsLowering] but processes local functions.
 * Useful for Kotlin/JS IR backend which keeps local declarations up until code generation.
 */
class AddContinuationToLocalSuspendFunctionsLowering(val context: JsCommonBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
                declaration.transformChildrenVoid()
                return if (declaration.isSuspend) {
                    transformSuspendFunction(context, declaration)
                } else {
                    declaration
                }
            }
        })
    }
}


private fun transformSuspendFunction(context: JsCommonBackendContext, function: IrSimpleFunction): IrSimpleFunction {
    val newFunctionWithContinuation = function.getOrCreateFunctionWithContinuationStub(context)
    // Using custom mapping because number of parameters doesn't match
    val parameterMapping = function.explicitParameters.zip(newFunctionWithContinuation.explicitParameters).toMap()
    val newBody = function.moveBodyTo(newFunctionWithContinuation, parameterMapping)

    // Since we are changing return type to Any, function can no longer return unit implicitly.
    if (
        function.returnType == context.irBuiltIns.unitType &&
        newBody is IrBlockBody &&
        newBody.statements.lastOrNull() !is IrReturn
    ) {
        // Adding explicit return of Unit.
        newBody.statements += context.createIrBuilder(newFunctionWithContinuation.symbol).irReturnUnit()
    }

    newFunctionWithContinuation.body = newBody
    return newFunctionWithContinuation
}


fun IrSimpleFunction.getOrCreateFunctionWithContinuationStub(context: JsCommonBackendContext): IrSimpleFunction {
    return context.mapping.suspendFunctionsToFunctionWithContinuations.getOrPut(this) {
        createSuspendFunctionStub(context)
    }
}

private fun IrSimpleFunction.createSuspendFunctionStub(context: JsCommonBackendContext): IrSimpleFunction {
    require(this.isSuspend)
    return factory.buildFun {
        updateFrom(this@createSuspendFunctionStub)
        isSuspend = false
        name = this@createSuspendFunctionStub.name
        origin = IrDeclarationOrigin.LOWERED_SUSPEND_FUNCTION
        returnType = loweredSuspendFunctionReturnType(this@createSuspendFunctionStub, context.irBuiltIns)
    }.also { function ->
        function.parent = parent

        function.annotations += annotations
        function.metadata = metadata

        function.copyAttributes(this)
        function.copyTypeParametersFrom(this)
        val substitutionMap = makeTypeParameterSubstitutionMap(this, function)
        function.copyReceiverParametersFrom(this, substitutionMap)

        function.overriddenSymbols += overriddenSymbols.map {
            it.owner.getOrCreateFunctionWithContinuationStub(context).symbol
        }
        function.valueParameters = valueParameters.map { it.copyTo(function) }

        val mapping = mutableMapOf<IrValueSymbol, IrValueSymbol>()
        valueParameters.forEach { mapping[it.symbol] = function.valueParameters[it.index].symbol }
        val remapper = ValueRemapper(mapping)
        function.valueParameters.forEach { it.defaultValue = it.defaultValue?.transform(remapper, null) }

        function.addValueParameter(
            "\$cont",
            continuationType(context).substitute(substitutionMap),
            IrDeclarationOrigin.CONTINUATION
        )
    }
}

private fun IrFunction.continuationType(context: JsCommonBackendContext): IrType {
    return context.coroutineSymbols.continuationClass.typeWith(returnType)
}