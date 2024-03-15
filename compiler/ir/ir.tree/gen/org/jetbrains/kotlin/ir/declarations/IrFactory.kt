/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.Variance

interface IrFactory {
    val stageController: StageController

    fun createAnonymousInitializer(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrAnonymousInitializerSymbol,
        isStatic: Boolean = false,
    ): IrAnonymousInitializer

    fun createClass(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        symbol: IrClassSymbol,
        kind: ClassKind,
        modality: Modality,
        isExternal: Boolean = false,
        isCompanion: Boolean = false,
        isInner: Boolean = false,
        isData: Boolean = false,
        isValue: Boolean = false,
        isExpect: Boolean = false,
        isFun: Boolean = false,
        hasEnumEntries: Boolean = false,
        source: SourceElement = SourceElement.NO_SOURCE,
    ): IrClass

    fun createConstructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        isInline: Boolean,
        isExpect: Boolean,
        returnType: IrType?,
        symbol: IrConstructorSymbol,
        isPrimary: Boolean,
        isExternal: Boolean = false,
        containerSource: DeserializedContainerSource? = null,
    ): IrConstructor

    fun createEnumEntry(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        symbol: IrEnumEntrySymbol,
    ): IrEnumEntry

    fun createErrorDeclaration(
        startOffset: Int,
        endOffset: Int,
        descriptor: DeclarationDescriptor? = null,
    ): IrErrorDeclaration

    fun createField(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        symbol: IrFieldSymbol,
        type: IrType,
        isFinal: Boolean,
        isStatic: Boolean,
        isExternal: Boolean = false,
    ): IrField

    fun createFunctionWithLateBinding(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        isInline: Boolean,
        isExpect: Boolean,
        returnType: IrType?,
        modality: Modality,
        isTailrec: Boolean,
        isSuspend: Boolean,
        isOperator: Boolean,
        isInfix: Boolean,
        isExternal: Boolean = false,
        isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
    ): IrFunctionWithLateBinding

    fun createLocalDelegatedProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        symbol: IrLocalDelegatedPropertySymbol,
        type: IrType,
        isVar: Boolean,
    ): IrLocalDelegatedProperty

    fun createProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        modality: Modality,
        symbol: IrPropertySymbol,
        isVar: Boolean,
        isConst: Boolean,
        isLateinit: Boolean,
        isDelegated: Boolean,
        isExternal: Boolean = false,
        containerSource: DeserializedContainerSource? = null,
        isExpect: Boolean = false,
        isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
    ): IrProperty

    fun createPropertyWithLateBinding(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        modality: Modality,
        isVar: Boolean,
        isConst: Boolean,
        isLateinit: Boolean,
        isDelegated: Boolean,
        isExternal: Boolean = false,
        isExpect: Boolean = false,
        isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
    ): IrPropertyWithLateBinding

    fun createSimpleFunction(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        isInline: Boolean,
        isExpect: Boolean,
        returnType: IrType?,
        modality: Modality,
        symbol: IrSimpleFunctionSymbol,
        isTailrec: Boolean,
        isSuspend: Boolean,
        isOperator: Boolean,
        isInfix: Boolean,
        isExternal: Boolean = false,
        containerSource: DeserializedContainerSource? = null,
        isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
    ): IrSimpleFunction

    fun createTypeAlias(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        symbol: IrTypeAliasSymbol,
        isActual: Boolean,
        expandedType: IrType,
    ): IrTypeAlias

    fun createTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        symbol: IrTypeParameterSymbol,
        variance: Variance,
        index: Int,
        isReified: Boolean,
    ): IrTypeParameter

    fun createValueParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        type: IrType,
        isAssignable: Boolean,
        symbol: IrValueParameterSymbol,
        index: Int,
        varargElementType: IrType?,
        isCrossinline: Boolean,
        isNoinline: Boolean,
        isHidden: Boolean,
    ): IrValueParameter

    fun createBlockBody(startOffset: Int, endOffset: Int): IrBlockBody

    fun createExpressionBody(
        startOffset: Int,
        endOffset: Int,
        expression: IrExpression,
    ): IrExpressionBody
}
