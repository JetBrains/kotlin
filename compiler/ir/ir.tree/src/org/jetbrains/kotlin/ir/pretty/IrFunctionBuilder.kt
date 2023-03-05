/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind

@PrettyIrDsl
abstract class IrFunctionBuilder<Function : IrFunction> internal constructor(buildingContext: IrBuildingContext) :
    IrDeclarationBuilder<Function>(buildingContext),
    IrDeclarationWithVisibilityBuilder,
    IrSymbolOwnerBuilder,
    IrPossiblyExternalDeclarationBuilder {

    override var declarationVisibility: DescriptorVisibility by SetAtMostOnce(DescriptorVisibilities.DEFAULT_VISIBILITY)

    override var symbolReference: String? by SetAtMostOnce(null)

    override var isExternal: Boolean by SetAtMostOnce(false)

    protected var isInline by SetAtMostOnce(false)

    @IrNodePropertyDsl
    fun inline(isInline: Boolean = true) {
        this.isInline = isInline
    }

    protected var isExpect by SetAtMostOnce(false)

    @IrNodePropertyDsl
    fun expect(isExpect: Boolean = true) {
        this.isExpect = isExpect
    }

    @PublishedApi
    internal var bodyBuilder: IrBodyBuilder<*>? by SetAtMostOnce(null)

    @IrNodeBuilderDsl
    inline fun irBlockBody(block: IrBlockBodyBuilder.() -> Unit) {
        bodyBuilder = IrBlockBodyBuilder(buildingContext).apply(block)
    }

    @IrNodeBuilderDsl
    inline fun irExpressionBody(block: IrExpressionBodyBuilder.() -> Unit) {
        bodyBuilder = IrExpressionBodyBuilder(buildingContext).apply(block)
    }

    @IrNodeBuilderDsl
    inline fun irSyntheticBody(kind: IrSyntheticBodyKind, block: IrSyntheticBodyBuilder.() -> Unit = {}) {
        bodyBuilder = IrSyntheticBodyBuilder(kind, buildingContext).apply(block)
    }
}

internal fun <Function : IrFunction> IrFunctionBuilder<Function>.addFunctionPropertiesTo(function: Function) {
    recordSymbolFromOwner(function)
    addAnnotationsTo(function)
    // TODO: typeParameters
    // TODO: dispatchReceiverParameter
    // TODO: extensionReceiverParameter
    // TODO: valueParameters
    // TODO: contextReceiverParametersCount
    function.body = bodyBuilder?.build()
}
