/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.serialization.NonLinkingIrInlineFunctionDeserializer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.isPrivate
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*

/**
 * Checks if the given function should be treated by 1st phase of inlining (inlining of private functions):
 * - Either the function is private.
 * - Or the function is declared inside a local class.
 */
fun IrFunctionSymbol.isConsideredAsPrivateForInlining(): Boolean = this.isBound && (isPrivate(owner.visibility) || owner.isLocal)

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
    fun at(scope: Scope, expression: IrExpression) {}

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
    ALL_FUNCTIONS,
}

interface InlineFunctionResolver {
    val inlineMode: InlineMode
    val callInlinerStrategy: CallInlinerStrategy
    val allowExternalInlining: Boolean
    fun needsInlining(symbol: IrFunctionSymbol): Boolean
    fun needsInlining(expression: IrFunctionAccessExpression): Boolean
    fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction?
}

abstract class AbstractInlineFunctionResolver(override val inlineMode: InlineMode) : InlineFunctionResolver {
    override val callInlinerStrategy: CallInlinerStrategy
        get() = CallInlinerStrategy.DEFAULT
    override val allowExternalInlining: Boolean
        get() = false

    override fun needsInlining(symbol: IrFunctionSymbol) =
        symbol.isBound && symbol.owner.isInline && (allowExternalInlining || !symbol.owner.isExternal)

    override fun needsInlining(expression: IrFunctionAccessExpression) = needsInlining(expression.symbol)

    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        if (shouldExcludeFunctionFromInlining(symbol)) return null

        val owner = symbol.owner
        return (owner as? IrSimpleFunction)?.resolveFakeOverride() ?: owner
    }

    protected open fun shouldExcludeFunctionFromInlining(symbol: IrFunctionSymbol): Boolean {
        return !needsInlining(symbol) || Symbols.isTypeOfIntrinsic(symbol)
    }
}

abstract class InlineFunctionResolverReplacingCoroutineIntrinsics<Ctx : LoweringContext>(
    protected val context: Ctx,
    inlineMode: InlineMode,
) : AbstractInlineFunctionResolver(inlineMode) {
    final override val allowExternalInlining: Boolean
        get() = context.allowExternalInlining

    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        val function = super.getFunctionDeclaration(symbol) ?: return null
        // TODO: Remove these hacks when coroutine intrinsics are fixed.
        return when {
            function.isBuiltInSuspendCoroutineUninterceptedOrReturn() ->
                context.ir.symbols.suspendCoroutineUninterceptedOrReturn.owner

            symbol == context.ir.symbols.coroutineContextGetter ->
                context.ir.symbols.coroutineGetContext.owner

            else -> function
        }
    }

    override fun shouldExcludeFunctionFromInlining(symbol: IrFunctionSymbol): Boolean {
        return super.shouldExcludeFunctionFromInlining(symbol) ||
                (inlineMode == InlineMode.PRIVATE_INLINE_FUNCTIONS && !symbol.isConsideredAsPrivateForInlining())
    }
}

/**
 * These resolvers are supposed to be run at the first compilation stage for all non-JVM targets.
 */
internal class PreSerializationPrivateInlineFunctionResolver(
    context: LoweringContext,
) : InlineFunctionResolverReplacingCoroutineIntrinsics<LoweringContext>(context, InlineMode.PRIVATE_INLINE_FUNCTIONS) {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        val function = super.getFunctionDeclaration(symbol)
        if (function != null) {
            check(function.body != null) { "Unexpected inline function without body: ${function.render()}" }
        }
        return function
    }
}

internal class PreSerializationNonPrivateInlineFunctionResolver(
    context: LoweringContext,
    irMangler: KotlinMangler.IrMangler,
) : InlineFunctionResolverReplacingCoroutineIntrinsics<LoweringContext>(context, InlineMode.ALL_INLINE_FUNCTIONS) {

    private val deserializer = NonLinkingIrInlineFunctionDeserializer(
        irBuiltIns = context.irBuiltIns,
        signatureComputer = PublicIdSignatureComputer(irMangler)
    )

    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        val function = super.getFunctionDeclaration(symbol)
        if (function != null && function.body == null) {
            deserializer.deserializeInlineFunction(function)
        }
        return function
    }
}