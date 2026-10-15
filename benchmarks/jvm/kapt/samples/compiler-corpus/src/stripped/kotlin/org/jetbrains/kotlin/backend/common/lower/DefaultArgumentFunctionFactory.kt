/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.defaultArgumentsDispatchFunction
import org.jetbrains.kotlin.backend.common.defaultArgumentsOriginalFunction
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

abstract class DefaultArgumentFunctionFactory(
    val context: CommonBackendContext,
    val copyOriginalFunctionLocation: Boolean = true,
) {

    protected fun IrFunction.generateDefaultArgumentsFunctionName() =
        Name.identifier("${name}\$default")

    protected abstract fun IrFunction.generateDefaultArgumentStubFrom(original: IrFunction, useConstructorMarker: Boolean)

    protected fun IrFunction.copyReturnTypeFrom(original: IrFunction) { TODO() }

    /**
     * Whether `null` will be used for this type if no argument is passed.
     * In that case, the type of the default dispatch function will be made nullable.
     *
     * By default, always returns `true` – this is valid, but suboptimal.
     * Better performance can be achieved in a backend-specific way.
     */
    protected open fun IrType.hasNullAsUndefinedValue(): Boolean = TODO()

    /**
     * Annotations of the original function to keep on the generated default stub.
     *
     * By default, all of them are kept.
     * TODO some annotations are needed (e.g. @JvmStatic), others need different values (e.g. @JvmName), the rest are redundant.
     */
    protected open fun filterCopiedAnnotations(annotations: List<IrAnnotation>): List<IrAnnotation> = TODO()

    protected fun IrFunction.copyValueParametersFrom(original: IrFunction) { TODO() }

    protected open val IrFunction.hasDefaultValues: Boolean
        get() = parameters.any { it.defaultValue != null }

    fun findBaseFunctionWithDefaultArgumentsFor(
        declaration: IrFunction,
        skipInlineMethods: Boolean,
        skipExternalMethods: Boolean
    ): IrFunction? { TODO() }


    fun generateDefaultsFunction(
        declaration: IrFunction,
        skipInlineMethods: Boolean,
        skipExternalMethods: Boolean,
        forceSetOverrideSymbols: Boolean,
        visibility: DescriptorVisibility,
        useConstructorMarker: Boolean,
        copiedAnnotations: List<IrAnnotation>,
    ): IrFunction? { TODO() }


    private fun generateDefaultsFunctionImpl(
        declaration: IrFunction,
        newOrigin: IrDeclarationOrigin,
        newVisibility: DescriptorVisibility,
        copiedAnnotations: List<IrAnnotation>,
        isFakeOverride: Boolean,
        useConstructorMarker: Boolean,
    ): IrFunction { TODO() }
}
