/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.persistent

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.persistent.PersistentIrBlockBody
import org.jetbrains.kotlin.ir.expressions.persistent.PersistentIrExpressionBody
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.Variance

object PersistentIrFactory : IrFactory {
    override fun createAnonymousInitializer(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrAnonymousInitializerSymbol,
        isStatic: Boolean,
    ): IrAnonymousInitializer =
        PersistentIrAnonymousInitializer(startOffset, endOffset, origin, symbol, isStatic)

    override fun createClass(
            startOffset: Int,
            endOffset: Int,
            origin: IrDeclarationOrigin,
            symbol: IrClassSymbol,
            name: Name,
            kind: ClassKind,
            visibility: DescriptorVisibility,
            modality: Modality,
            isCompanion: Boolean,
            isInner: Boolean,
            isData: Boolean,
            isExternal: Boolean,
            isInline: Boolean,
            isExpect: Boolean,
            isFun: Boolean,
            source: SourceElement,
    ): IrClass =
        PersistentIrClass(
            startOffset, endOffset, origin, symbol, name, kind, visibility, modality,
            isCompanion, isInner, isData, isExternal, isInline, isExpect, isFun, source,
        )

    override fun createConstructor(
            startOffset: Int,
            endOffset: Int,
            origin: IrDeclarationOrigin,
            symbol: IrConstructorSymbol,
            name: Name,
            visibility: DescriptorVisibility,
            returnType: IrType,
            isInline: Boolean,
            isExternal: Boolean,
            isPrimary: Boolean,
            isExpect: Boolean,
            containerSource: DeserializedContainerSource?,
    ): IrConstructor =
        PersistentIrConstructor(
            startOffset, endOffset, origin, symbol, name, visibility, returnType, isInline, isExternal, isPrimary, isExpect,
            containerSource,
        )

    override fun createEnumEntry(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrEnumEntrySymbol,
        name: Name,
    ): IrEnumEntry =
        PersistentIrEnumEntry(startOffset, endOffset, origin, symbol, name)

    override fun createErrorDeclaration(
        startOffset: Int,
        endOffset: Int,
        descriptor: DeclarationDescriptor,
    ): IrErrorDeclaration =
        PersistentIrErrorDeclaration(startOffset, endOffset, descriptor)

    override fun createField(
            startOffset: Int,
            endOffset: Int,
            origin: IrDeclarationOrigin,
            symbol: IrFieldSymbol,
            name: Name,
            type: IrType,
            visibility: DescriptorVisibility,
            isFinal: Boolean,
            isExternal: Boolean,
            isStatic: Boolean,
    ): IrField =
        PersistentIrField(startOffset, endOffset, origin, symbol, name, type, visibility, isFinal, isExternal, isStatic)

    override fun createFunction(
            startOffset: Int,
            endOffset: Int,
            origin: IrDeclarationOrigin,
            symbol: IrSimpleFunctionSymbol,
            name: Name,
            visibility: DescriptorVisibility,
            modality: Modality,
            returnType: IrType,
            isInline: Boolean,
            isExternal: Boolean,
            isTailrec: Boolean,
            isSuspend: Boolean,
            isOperator: Boolean,
            isInfix: Boolean,
            isExpect: Boolean,
            isFakeOverride: Boolean,
            containerSource: DeserializedContainerSource?,
    ): IrSimpleFunction =
        PersistentIrFunction(
            startOffset, endOffset, origin, symbol, name, visibility, modality, returnType,
            isInline, isExternal, isTailrec, isSuspend, isOperator, isInfix, isExpect, isFakeOverride,
            containerSource
        )

    override fun createFakeOverrideFunction(
            startOffset: Int,
            endOffset: Int,
            origin: IrDeclarationOrigin,
            name: Name,
            visibility: DescriptorVisibility,
            modality: Modality,
            returnType: IrType,
            isInline: Boolean,
            isExternal: Boolean,
            isTailrec: Boolean,
            isSuspend: Boolean,
            isOperator: Boolean,
            isInfix: Boolean,
            isExpect: Boolean,
    ): IrSimpleFunction =
        PersistentIrFakeOverrideFunction(
            startOffset, endOffset, origin, name, visibility, modality, returnType,
            isInline, isExternal, isTailrec, isSuspend, isOperator, isInfix, isExpect,
        )

    override fun createLocalDelegatedProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrLocalDelegatedPropertySymbol,
        name: Name,
        type: IrType,
        isVar: Boolean,
    ): IrLocalDelegatedProperty =
        PersistentIrLocalDelegatedProperty(
            startOffset, endOffset, origin, symbol, name, type, isVar
        )

    override fun createProperty(
            startOffset: Int,
            endOffset: Int,
            origin: IrDeclarationOrigin,
            symbol: IrPropertySymbol,
            name: Name,
            visibility: DescriptorVisibility,
            modality: Modality,
            isVar: Boolean,
            isConst: Boolean,
            isLateinit: Boolean,
            isDelegated: Boolean,
            isExternal: Boolean,
            isExpect: Boolean,
            isFakeOverride: Boolean,
            containerSource: DeserializedContainerSource?,
    ): IrProperty =
        PersistentIrProperty(
            startOffset, endOffset, origin, symbol, name, visibility, modality,
            isVar, isConst, isLateinit, isDelegated, isExternal, isExpect, isFakeOverride,
            containerSource
        )

    override fun createFakeOverrideProperty(
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
            isExternal: Boolean,
            isExpect: Boolean,
    ): IrProperty =
        PersistentIrFakeOverrideProperty(
            startOffset, endOffset, origin, name, visibility, modality,
            isVar, isConst, isLateinit, isDelegated, isExternal, isExpect,
        )

    override fun createTypeAlias(
            startOffset: Int,
            endOffset: Int,
            symbol: IrTypeAliasSymbol,
            name: Name,
            visibility: DescriptorVisibility,
            expandedType: IrType,
            isActual: Boolean,
            origin: IrDeclarationOrigin,
    ): IrTypeAlias =
        PersistentIrTypeAlias(startOffset, endOffset, symbol, name, visibility, expandedType, isActual, origin)

    override fun createTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrTypeParameterSymbol,
        name: Name,
        index: Int,
        isReified: Boolean,
        variance: Variance,
    ): IrTypeParameter =
        PersistentIrTypeParameter(startOffset, endOffset, origin, symbol, name, index, isReified, variance)

    override fun createValueParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrValueParameterSymbol,
        name: Name,
        index: Int,
        type: IrType,
        varargElementType: IrType?,
        isCrossinline: Boolean,
        isNoinline: Boolean,
        isHidden: Boolean,
        isAssignable: Boolean
    ): IrValueParameter =
        PersistentIrValueParameter(
            startOffset, endOffset, origin, symbol, name, index, type, varargElementType, isCrossinline, isNoinline, isHidden, isAssignable
        )

    override fun createExpressionBody(
        startOffset: Int,
        endOffset: Int,
        initializer: IrExpressionBody.() -> Unit,
    ): IrExpressionBody =
        PersistentIrExpressionBody(startOffset, endOffset, initializer)

    override fun createExpressionBody(
        startOffset: Int,
        endOffset: Int,
        expression: IrExpression,
    ): IrExpressionBody =
        PersistentIrExpressionBody(startOffset, endOffset, expression)

    override fun createExpressionBody(
        expression: IrExpression,
    ): IrExpressionBody =
        PersistentIrExpressionBody(expression)

    override fun createBlockBody(
        startOffset: Int,
        endOffset: Int,
    ): IrBlockBody =
        PersistentIrBlockBody(startOffset, endOffset)

    override fun createBlockBody(
        startOffset: Int,
        endOffset: Int,
        statements: List<IrStatement>,
    ): IrBlockBody =
        PersistentIrBlockBody(startOffset, endOffset, statements)

    override fun createBlockBody(
        startOffset: Int,
        endOffset: Int,
        initializer: IrBlockBody.() -> Unit,
    ): IrBlockBody =
        PersistentIrBlockBody(startOffset, endOffset, initializer)
}
