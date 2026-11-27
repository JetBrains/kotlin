/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:JvmName("IrExpressionBuilders")
@file:JvmMultifileClass
@file:OptIn(ExperimentalContracts::class)

package org.jetbrains.kotlin.ir.builders.expressions.v2

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrRichFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol

class IrRichFunctionReferenceBuilder @PublishedApi internal constructor() : IrRichCallableReferenceBuilder<IrFunctionSymbol>() {
    lateinit var overriddenFunctionSymbol: IrSimpleFunctionSymbol

    lateinit var invokeFunction: IrSimpleFunction

    var hasUnitConversion: Boolean = false

    var hasSuspendConversion: Boolean = false

    var hasVarargConversion: Boolean = false

    var isRestrictedSuspension: Boolean = false
}

@PublishedApi
internal fun build(builder: IrRichFunctionReferenceBuilder): IrRichFunctionReference {
    val node = IrRichFunctionReferenceImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        reflectionTargetSymbol = builder.reflectionTargetSymbol,
        origin = builder.origin,
        overriddenFunctionSymbol = builder.overriddenFunctionSymbol,
        invokeFunction = builder.invokeFunction,
        hasUnitConversion = builder.hasUnitConversion,
        hasSuspendConversion = builder.hasSuspendConversion,
        hasVarargConversion = builder.hasVarargConversion,
        isRestrictedSuspension = builder.isRestrictedSuspension,
    )
    node.boundValues.addAll(builder.boundValues)
    return node
}

inline fun buildRichFunctionReference(builder: IrRichFunctionReferenceBuilder.() -> Unit): IrRichFunctionReference {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrRichFunctionReferenceBuilder().apply(builder))
}

inline fun IrBuilder.irRichFunctionReference(builder: IrRichFunctionReferenceBuilder.() -> Unit): IrRichFunctionReference {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(richFunctionReferenceBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.richFunctionReferenceBuilder(): IrRichFunctionReferenceBuilder =
    IrRichFunctionReferenceBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
