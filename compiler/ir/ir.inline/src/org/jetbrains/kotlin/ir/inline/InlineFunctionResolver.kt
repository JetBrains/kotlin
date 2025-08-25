/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.serialization.NonLinkingIrInlineFunctionDeserializer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
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

enum class InlineMode {
    PRIVATE_INLINE_FUNCTIONS,
    INTRA_MODULE_INLINE_FUNCTIONS,
    ALL_INLINE_FUNCTIONS,
}

abstract class InlineFunctionResolver() {
    protected open fun shouldSkipBecauseOfCallSite(expression: IrMemberAccessExpression<IrFunctionSymbol>) = false

    protected abstract fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction?

    fun getFunctionDeclarationToInline(expression: IrMemberAccessExpression<IrFunctionSymbol>): IrFunction? {
        if (shouldSkipBecauseOfCallSite(expression)) return null
        return getFunctionDeclaration(expression.symbol)
    }
}

abstract class InlineFunctionResolverReplacingCoroutineIntrinsics<Ctx : LoweringContext>(
    protected val context: Ctx,
    protected val inlineMode: InlineMode,
) : InlineFunctionResolver() {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        if (!symbol.isBound) return null
        val realOwner = symbol.owner.resolveFakeOverrideOrSelf()
        if (!realOwner.isInline) return null
        val result = when {
            realOwner.isBuiltInSuspendCoroutineUninterceptedOrReturn() -> context.symbols.suspendCoroutineUninterceptedOrReturn.owner
            realOwner.symbol == context.symbols.coroutineContextGetter -> context.symbols.coroutineGetContext.owner
            else -> realOwner
        }
        if (inlineMode == InlineMode.PRIVATE_INLINE_FUNCTIONS && !result.isEffectivelyPrivate()) return null
        return result
    }
}

/**
 * These resolvers are supposed to be run at the first compilation stage for all non-JVM targets.
 */
internal class PreSerializationPrivateInlineFunctionResolver(
    context: LoweringContext,
) : InlineFunctionResolverReplacingCoroutineIntrinsics<LoweringContext>(context, InlineMode.PRIVATE_INLINE_FUNCTIONS) {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        return super.getFunctionDeclaration(symbol)?.also { function ->
            check(function.body != null) { "Unexpected inline function without body: ${function.render()}" }
        }
    }
}

internal class PreSerializationNonPrivateInlineFunctionResolver(
    context: LoweringContext,
    irMangler: KotlinMangler.IrMangler,
    inlineCrossModuleFunctions: Boolean,
) : InlineFunctionResolverReplacingCoroutineIntrinsics<LoweringContext>(
    context,
    if (inlineCrossModuleFunctions) InlineMode.ALL_INLINE_FUNCTIONS else InlineMode.INTRA_MODULE_INLINE_FUNCTIONS
) {

    private val deserializer = NonLinkingIrInlineFunctionDeserializer(
        irBuiltIns = context.irBuiltIns,
        signatureComputer = PublicIdSignatureComputer(irMangler)
    )

    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        val declarationMaybeFromOtherModule = super.getFunctionDeclaration(symbol) ?: return null
        if (declarationMaybeFromOtherModule.body != null) {
            return declarationMaybeFromOtherModule
        }
        if (inlineMode != InlineMode.ALL_INLINE_FUNCTIONS) return null
        return deserializer.deserializeInlineFunction(declarationMaybeFromOtherModule)
    }
}