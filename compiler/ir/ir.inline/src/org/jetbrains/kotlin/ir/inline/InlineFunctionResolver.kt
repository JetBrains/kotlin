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
        if (inlineMode == InlineMode.PRIVATE_INLINE_FUNCTIONS && !realOwner.isEffectivelyPrivate()) return null
        if (!context.allowExternalInlining && realOwner.isExternal) return null
        return realOwner
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
) : InlineFunctionResolverReplacingCoroutineIntrinsics<LoweringContext>(context, InlineMode.ALL_INLINE_FUNCTIONS) {

    private val deserializer = NonLinkingIrInlineFunctionDeserializer(
        irBuiltIns = context.irBuiltIns,
        signatureComputer = PublicIdSignatureComputer(irMangler)
    )

    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        val declarationMaybeFromOtherModule = super.getFunctionDeclaration(symbol) ?: return null
        if (declarationMaybeFromOtherModule.body != null) {
            return declarationMaybeFromOtherModule
        }
        return deserializer.deserializeInlineFunction(declarationMaybeFromOtherModule)
    }
}