/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrRichFunctionReferenceImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    reflectionTargetSymbol: IrFunctionSymbol?,
    origin: IrStatementOrigin?,
    overriddenFunctionSymbol: IrSimpleFunctionSymbol,
    invokeFunction: IrSimpleFunction,
    hasUnitConversion: Boolean,
    hasSuspendConversion: Boolean,
    hasVarargConversion: Boolean,
    isRestrictedSuspension: Boolean,
) : IrRichFunctionReference() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var type: IrType by typeAttribute
    override var reflectionTargetSymbol: IrFunctionSymbol? by reflectionTargetSymbolAttribute
    override val boundValues: MutableList<IrExpression> by boundValuesAttribute
    override var origin: IrStatementOrigin? by originAttribute
    override var overriddenFunctionSymbol: IrSimpleFunctionSymbol by overriddenFunctionSymbolAttribute
    override var invokeFunction: IrSimpleFunction by invokeFunctionAttribute
    override var hasUnitConversion: Boolean by hasUnitConversionAttribute
    override var hasSuspendConversion: Boolean by hasSuspendConversionAttribute
    override var hasVarargConversion: Boolean by hasVarargConversionAttribute
    override var isRestrictedSuspension: Boolean by isRestrictedSuspensionAttribute

    init {
        preallocateStorage(8)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(reflectionTargetSymbolAttribute, reflectionTargetSymbol)
        initAttribute(originAttribute, origin)
        initAttribute(boundValuesAttribute, ArrayList())
        initAttribute(overriddenFunctionSymbolAttribute, overriddenFunctionSymbol)
        initAttribute(typeAttribute, type)
        initAttribute(invokeFunctionAttribute, invokeFunction)
        if (isRestrictedSuspension) setFlagInternal(isRestrictedSuspensionAttribute, true)
        if (hasVarargConversion) setFlagInternal(hasVarargConversionAttribute, true)
        if (hasSuspendConversion) setFlagInternal(hasSuspendConversionAttribute, true)
        if (hasUnitConversion) setFlagInternal(hasUnitConversionAttribute, true)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrRichFunctionReferenceImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrRichFunctionReferenceImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrRichFunctionReferenceImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val typeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType>(IrRichFunctionReferenceImpl::class.java, 7, "type", null)
        @JvmStatic private val reflectionTargetSymbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFunctionSymbol?>(IrRichFunctionReferenceImpl::class.java, 3, "reflectionTargetSymbol", null)
        @JvmStatic private val boundValuesAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrExpression>>(IrRichFunctionReferenceImpl::class.java, 5, "boundValues", null)
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrStatementOrigin?>(IrRichFunctionReferenceImpl::class.java, 4, "origin", null)
        @JvmStatic private val overriddenFunctionSymbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrSimpleFunctionSymbol>(IrRichFunctionReferenceImpl::class.java, 6, "overriddenFunctionSymbol", null)
        @JvmStatic private val invokeFunctionAttribute = IrIndexBasedAttributeRegistry.createAttr<IrSimpleFunction>(IrRichFunctionReferenceImpl::class.java, 8, "invokeFunction", null)
        @JvmStatic private val hasUnitConversionAttribute = IrIndexBasedAttributeRegistry.createFlag(IrRichFunctionReferenceImpl::class.java, 63, "hasUnitConversion")
        @JvmStatic private val hasSuspendConversionAttribute = IrIndexBasedAttributeRegistry.createFlag(IrRichFunctionReferenceImpl::class.java, 62, "hasSuspendConversion")
        @JvmStatic private val hasVarargConversionAttribute = IrIndexBasedAttributeRegistry.createFlag(IrRichFunctionReferenceImpl::class.java, 61, "hasVarargConversion")
        @JvmStatic private val isRestrictedSuspensionAttribute = IrIndexBasedAttributeRegistry.createFlag(IrRichFunctionReferenceImpl::class.java, 60, "isRestrictedSuspension")
    }
}
