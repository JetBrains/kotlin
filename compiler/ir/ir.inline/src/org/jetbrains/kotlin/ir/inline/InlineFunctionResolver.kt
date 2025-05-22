/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.serialization.NonLinkingIrInlineFunctionDeserializer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*

/**
 * Checks if the given function should be treated by 1st phase of inlining (inlining of private functions)
 */
fun IrFunctionSymbol.isConsideredAsPrivateForInlining(): Boolean = this.isBound && owner.resolveFakeOverrideOrSelf().isEffectivelyPrivate()

/**
 * Checks if the given function is available in the inlined scope.
 * - Effectively private declarations are not available (outside the given file).
 * - Local declarations are always fine because they will be copied with inline declaration.
 */
fun IrFunctionSymbol.isConsideredAsPrivateAndNotLocalForInlining(): Boolean = this.isBound && owner.isEffectivelyPrivate() && !owner.isLocal

interface CallInlinerStrategy {
    /**
     * TypeOf function requires some custom backend-specific processing. This is a customization point for that.
     *
     * @param expression is a copy of original IrCall with types substituted by normal rules
     * @param nonSubstitutedTypeArgument is typeArgument of call with only reified type parameters substituted
     *
     * @return new node to insert instead of typeOf call.
     */
    fun postProcessTypeOf(expression: IrCall, nonSubstitutedTypeArgument: IrType): IrExpression
    fun at(container: IrDeclaration, expression: IrExpression) {}

    object DEFAULT : CallInlinerStrategy {
        override fun postProcessTypeOf(expression: IrCall, nonSubstitutedTypeArgument: IrType): IrExpression {
            return expression.apply {
                typeArguments[0] = nonSubstitutedTypeArgument
            }
        }
    }
}

enum class InlineMode {
    PRIVATE_INLINE_FUNCTIONS,
    ALL_INLINE_FUNCTIONS,
}

abstract class InlineFunctionResolver(
    val callInlinerStrategy: CallInlinerStrategy = CallInlinerStrategy.DEFAULT,
) {
    protected open fun shouldSkipBecauseOfCallSite(expression: IrFunctionAccessExpression) = false

    protected abstract fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction?

    fun getFunctionDeclarationToInline(expression: IrFunctionAccessExpression): IrFunction? {
        if (shouldSkipBecauseOfCallSite(expression)) return null
        return getFunctionDeclaration(expression.symbol)
    }

    fun needsInlining(expression: IrFunctionAccessExpression): Boolean {
        return getFunctionDeclarationToInline(expression) != null
    }

    fun needsInlining(function: IrFunction): Boolean {
        return getFunctionDeclaration(function.symbol) != null
    }
}

abstract class InlineFunctionResolverReplacingCoroutineIntrinsics<Ctx : LoweringContext>(
    protected val context: Ctx,
    private val inlineMode: InlineMode,
    callInlinerStrategy: CallInlinerStrategy = CallInlinerStrategy.DEFAULT,
) : InlineFunctionResolver(callInlinerStrategy) {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        if (!symbol.isBound) return null
        val realOwner = symbol.owner.resolveFakeOverrideOrSelf()
        if (!realOwner.isInline) return null
        // TODO: drop special cases KT-77111
        val result = when {
            realOwner.isBuiltInSuspendCoroutineUninterceptedOrReturn() -> context.symbols.suspendCoroutineUninterceptedOrReturn.owner
            realOwner.symbol == context.symbols.coroutineContextGetter -> context.symbols.coroutineGetContext.owner
            else -> realOwner
        }
        if (inlineMode == InlineMode.PRIVATE_INLINE_FUNCTIONS && !result.isEffectivelyPrivate()) return null
        if (!context.allowExternalInlining && result.isExternal) return null
        return result
    }
}