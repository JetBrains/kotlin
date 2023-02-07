/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.backend.common.lower.DefaultParameterInjector
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.web.WebStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.backend.js.utils.jsConstructorReference
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.copyAnnotations
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.ir.util.isVararg

class JsDefaultParameterInjector(override val context: JsIrBackendContext) :
    DefaultParameterInjector(
        context,
        skipExternalMethods = true,
        forceSetOverrideSymbols = false,
        factory = JsDefaultArgumentFunctionFactory(context)
    ) {
    override fun nullConst(startOffset: Int, endOffset: Int, irParameter: IrValueParameter): IrExpression? =
        if (irParameter.isVararg && !irParameter.hasDefaultValue()) {
            null
        } else {
            context.getVoid()
        }

    override fun shouldReplaceWithSyntheticFunction(functionAccess: IrFunctionAccessExpression): Boolean {
        return super.shouldReplaceWithSyntheticFunction(functionAccess) || functionAccess.symbol.owner.run {
            origin == JsLoweredDeclarationOrigin.JS_SHADOWED_EXPORT &&
                    !isTopLevel &&
                    functionAccess.origin != WebStatementOrigins.IMPLEMENTATION_DELEGATION_CALL &&
                    isExported(context)
        }
    }

    override fun parametersForCall(expression: IrFunctionAccessExpression): Pair<IrFunctionSymbol, List<IrExpression?>>? {
        val startOffset = expression.startOffset
        val endOffset = expression.endOffset
        val declaration = expression.symbol.owner

        val stubFunction = factory.findBaseFunctionWithDefaultArgumentsFor(declaration, skipInline, skipExternalMethods)?.let {
            factory.generateDefaultsFunction(
                it,
                skipInline,
                skipExternalMethods,
                forceSetOverrideSymbols,
                defaultArgumentStubVisibility(declaration),
                useConstructorMarker(declaration),
                it.copyAnnotations(),
            )
        } ?: return null

        return stubFunction.symbol to buildList {
            for (i in 0 until expression.valueArgumentsCount) {
                val declaredParameter = stubFunction.valueParameters[i]
                val actualParameter = expression.getValueArgument(i)
                add(actualParameter ?: nullConst(startOffset, endOffset, declaredParameter))
            }

            if (expression is IrCall && stubFunction.hasSuperContextParameter()) {
                add(expression.superQualifierSymbol?.prototypeOf() ?: context.getVoid())
            }
        }
    }

    private fun IrFunction.hasSuperContextParameter(): Boolean {
        return valueParameters.lastOrNull()?.origin == JsLoweredDeclarationOrigin.JS_SUPER_CONTEXT_PARAMETER
    }

    private fun IrClassSymbol.prototypeOf(): IrExpression {
        return IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.dynamicType,
            context.intrinsics.jsPrototypeOfSymbol,
            0,
            1
        ).apply {
            putValueArgument(0, owner.jsConstructorReference(context))
        }
    }

    private fun IrValueParameter.hasDefaultValue(): Boolean =
        origin == JsLoweredDeclarationOrigin.JS_SHADOWED_DEFAULT_PARAMETER
}

