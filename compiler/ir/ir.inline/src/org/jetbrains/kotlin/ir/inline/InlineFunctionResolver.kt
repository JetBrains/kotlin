/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.serialization.NonLinkingIrInlineFunctionDeserializer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.inline.PreSerializationNonPrivateInlineFunctionResolver.Companion.EXCLUDED_FROM_FIRST_STAGE_INLINING_ANNOTATION_FQNAME
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName

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

abstract class InlineFunctionResolver(
    protected val inlineMode: InlineMode
) {
    /**
     * Find (resolve) the suitable [IrFunction] for inlining given [symbol] from the [IrMemberAccessExpression] node.
     * Return the resolved function or null if it cannot be inlined.
     */
    protected abstract fun findSuitableFunctionToInline(symbol: IrFunctionSymbol): IrFunction?

    /**
     * Pre-process the [function] found in [findSuitableFunctionToInline] before returning it in [getFunctionDeclarationToInline].
     */
    protected open fun preProcessFunctionToInline(function: IrFunction) {}

    /**
     * @param expression the expression that is used to call the function to be inlined.
     * @param expressionFile the file in which the expression is located.
     */
    fun getFunctionDeclarationToInline(
        expression: IrMemberAccessExpression<IrFunctionSymbol>,
        expressionFile: IrFile? = null,
    ): IrFunction? {
        val function = findSuitableFunctionToInline(expression.symbol) ?: return null

        if (inlineMode == InlineMode.PRIVATE_INLINE_FUNCTIONS) {
            checkNotNull(expressionFile) {
                "'expressionFile' should not be null for ${InlineMode.PRIVATE_INLINE_FUNCTIONS::class.simpleName} inlining mode"
            }
            if (function.fileOrNull != expressionFile) return null
        }

        preProcessFunctionToInline(function)
        return function
    }
}

abstract class InlineFunctionResolverReplacingCoroutineIntrinsics<Ctx : LoweringContext>(
    protected val context: Ctx,
    inlineMode: InlineMode,
) : InlineFunctionResolver(inlineMode) {
    override fun findSuitableFunctionToInline(symbol: IrFunctionSymbol): IrFunction? {
        if (!symbol.isBound) return null
        val realOwner = symbol.owner.resolveFakeOverrideOrSelf()
        if (!realOwner.isInline) return null
        return when {
            realOwner.isBuiltInSuspendCoroutineUninterceptedOrReturn() -> context.symbols.suspendCoroutineUninterceptedOrReturn.owner
            realOwner.symbol == context.symbols.coroutineContextGetter -> context.symbols.coroutineGetContext.owner
            else -> realOwner
        }
    }
}

internal abstract class PreSerializationInlineFunctionResolver(
    context: LoweringContext,
    inlineMode: InlineMode,
) : InlineFunctionResolverReplacingCoroutineIntrinsics<LoweringContext>(context, inlineMode) {
    override fun findSuitableFunctionToInline(symbol: IrFunctionSymbol): IrFunction? {
        val function = super.findSuitableFunctionToInline(symbol) ?: return null

        if (function.hasAnnotation(EXCLUDED_FROM_FIRST_STAGE_INLINING_ANNOTATION_FQNAME))
            return null

        return function
    }
}

/**
 * These resolvers are supposed to be run at the first compilation stage for all non-JVM targets.
 */
internal class PreSerializationPrivateInlineFunctionResolver(
    context: LoweringContext,
) : PreSerializationInlineFunctionResolver(context, InlineMode.PRIVATE_INLINE_FUNCTIONS) {
    override fun preProcessFunctionToInline(function: IrFunction) {
        check(function.body != null) { "Unexpected inline function without body: ${function.render()}" }
    }
}

internal class PreSerializationNonPrivateInlineFunctionResolver(
    context: PreSerializationLoweringContext,
    inlineCrossModuleFunctions: Boolean,
) : PreSerializationInlineFunctionResolver(
    context,
    if (inlineCrossModuleFunctions) InlineMode.ALL_INLINE_FUNCTIONS else InlineMode.INTRA_MODULE_INLINE_FUNCTIONS
) {

    private val deserializer = NonLinkingIrInlineFunctionDeserializer(
        irBuiltIns = context.irBuiltIns,
        signatureComputer = PublicIdSignatureComputer(context.irMangler)
    )

    override fun findSuitableFunctionToInline(symbol: IrFunctionSymbol): IrFunction? {
        val declarationMaybeFromOtherModule = super.findSuitableFunctionToInline(symbol) ?: return null

        if (declarationMaybeFromOtherModule.body != null || declarationMaybeFromOtherModule !is IrSimpleFunction) {
            return declarationMaybeFromOtherModule
        }

        if (inlineMode != InlineMode.ALL_INLINE_FUNCTIONS) return null
        return deserializer.deserializeInlineFunction(declarationMaybeFromOtherModule)
    }

    companion object {
        internal val EXCLUDED_FROM_FIRST_STAGE_INLINING_ANNOTATION_FQNAME = FqName("kotlin.internal.DoNotInlineOnFirstStage")
    }
}
