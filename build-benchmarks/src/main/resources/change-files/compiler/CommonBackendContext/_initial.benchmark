/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageSupportForLowerings
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

interface LoggingContext {
    var inVerbosePhase: Boolean
    fun log(message: () -> String)
}

interface ErrorReportingContext {
    fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean)
}

interface CommonBackendContext : BackendContext, LoggingContext, ErrorReportingContext {
    override val ir: Ir<CommonBackendContext>

    val configuration: CompilerConfiguration
    val scriptMode: Boolean

    fun throwUninitializedPropertyAccessException(builder: IrBuilderWithScope, name: String): IrExpression {
        val throwErrorFunction = ir.symbols.throwUninitializedPropertyAccessException.owner
        return builder.irCall(throwErrorFunction).apply {
            putValueArgument(0, builder.irString(name))
        }
    }

    val mapping: Mapping

    // Adjust internal structures after a deep copy of some declarations.
    fun handleDeepCopy(
        fileSymbolMap: MutableMap<IrFileSymbol, IrFileSymbol>,
        classSymbolMap: MutableMap<IrClassSymbol, IrClassSymbol>,
        functionSymbolMap: MutableMap<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>
    ) {}

    fun isSideEffectFree(call: IrCall): Boolean {
        return false
    }

    val preferJavaLikeCounterLoop: Boolean
        get() = false

    val doWhileCounterLoopOrigin: IrStatementOrigin?
        get() = null

    val inductionVariableOrigin: IrDeclarationOrigin
        get() = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE

    val optimizeLoopsOverUnsignedArrays: Boolean
        get() = false

    val optimizeNullChecksUsingKotlinNullability: Boolean
        get() = true

    fun remapMultiFieldValueClassStructure(
        oldFunction: IrFunction, newFunction: IrFunction,
        parametersMappingOrNull: Map<IrValueParameter, IrValueParameter>?
    ) = Unit

    /**
     * See [InlineClassesUtils].
     */
    val inlineClassesUtils: InlineClassesUtils
        get() = DefaultInlineClassesUtils

    val partialLinkageSupport: PartialLinkageSupportForLowerings
        get() = PartialLinkageSupportForLowerings.DISABLED
}

/**
 * Provides means for determining if a class should be treated as an inline/value class.
 *
 * In certain cases (for compatibility reasons) we don't want to mark a class as `inline`, but still want to treat it as one.
 *
 * See [org.jetbrains.kotlin.ir.backend.js.utils.JsInlineClassesUtils].
 */
interface InlineClassesUtils {
    /**
     * Should this class be treated as inline class?
     */
    fun isClassInlineLike(klass: IrClass): Boolean = klass.isSingleFieldValueClass

    /**
     * Unlike [org.jetbrains.kotlin.ir.util.getInlineClassUnderlyingType], doesn't use [IrClass.inlineClassRepresentation] because
     * for some reason it can be called for classes which are not inline, e.g. `kotlin.Double`.
     */
    fun getInlineClassUnderlyingType(irClass: IrClass): IrType =
        irClass.declarations.firstIsInstanceOrNull<IrConstructor>()?.takeIf { it.isPrimary }?.valueParameters?.get(0)?.type
            ?: error("Class has no primary constructor: ${irClass.fqNameWhenAvailable}")
}

object DefaultInlineClassesUtils : InlineClassesUtils
