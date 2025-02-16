/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.lower.InnerClassesSupport
import org.jetbrains.kotlin.backend.common.phaser.BackendContextHolder
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.isSingleFieldValueClass
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageSupportForLowerings
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

/**
 * A context that is used to pass data to the second stage compiler lowerings
 * (those that are executed after deserializing IR from KLIBs, or any lowering in the JVM backend).
 */
interface CommonBackendContext : LoweringContext, BackendContextHolder {
    val typeSystem: IrTypeSystemContext

    override val heldBackendContext: CommonBackendContext
        get() = this

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

    /**
     * See [InlineClassesUtils].
     */
    val inlineClassesUtils: InlineClassesUtils
        get() = DefaultInlineClassesUtils

    val partialLinkageSupport: PartialLinkageSupportForLowerings
        get() = PartialLinkageSupportForLowerings.DISABLED

    val innerClassesSupport: InnerClassesSupport
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
        irClass.declarations.firstIsInstanceOrNull<IrConstructor>()?.takeIf { it.isPrimary }?.parameters[0]?.type
            ?: error("Class has no primary constructor: ${irClass.fqNameWhenAvailable}")
}

object DefaultInlineClassesUtils : InlineClassesUtils
