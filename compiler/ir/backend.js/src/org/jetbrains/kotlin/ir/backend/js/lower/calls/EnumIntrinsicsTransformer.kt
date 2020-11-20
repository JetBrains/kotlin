/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.isStaticMethodOfClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.name.Name


class EnumIntrinsicsTransformer(private val context: JsIrBackendContext) : CallsTransformer {
    private fun transformEnumTopLevelIntrinsic(
        call: IrFunctionAccessExpression,
        staticMethodPredicate: (IrSimpleFunction) -> Boolean
    ): IrExpression {
        val enum = call.getTypeArgument(0)?.getClass() ?: return call
        if (!enum.isEnumClass) return call
        val staticMethod = enum.findDeclaration(staticMethodPredicate)
        if (staticMethod == null || !staticMethod.isStaticMethodOfClass)
            throw IllegalStateException("Enum class should have static method for ${call.symbol.owner.name}")

        return irCall(call, staticMethod.symbol)
    }

    private fun transformEnumValueOfIntrinsic(call: IrFunctionAccessExpression) = transformEnumTopLevelIntrinsic(call) {
        it.name == Name.identifier("valueOf") &&
                it.valueParameters.count() == 1 &&
                it.valueParameters[0].type.isString()
    }

    private fun transformEnumValuesIntrinsic(call: IrFunctionAccessExpression) = transformEnumTopLevelIntrinsic(call) {
        it.name == Name.identifier("values") && it.valueParameters.count() == 0
    }

    override fun transformFunctionAccess(call: IrFunctionAccessExpression, doNotIntrinsify: Boolean) = when (call.symbol) {
        context.intrinsics.enumValueOfIntrinsic -> transformEnumValueOfIntrinsic(call)
        context.intrinsics.enumValuesIntrinsic -> transformEnumValuesIntrinsic(call)
        else -> call
    }
}
