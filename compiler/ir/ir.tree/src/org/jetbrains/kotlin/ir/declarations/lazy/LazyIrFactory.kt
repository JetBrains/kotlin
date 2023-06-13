/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.Variance

// An IrFactory that does not recreate declarations for already bound symbols.
class LazyIrFactory(
    private val delegate: IrFactory,
) : IrFactory by delegate {
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
        isValue: Boolean,
        isExpect: Boolean,
        isFun: Boolean,
        source: SourceElement
    ): IrClass = if (symbol.isBound)
        symbol.owner
    else
        delegate.createClass(
            startOffset, endOffset, origin, symbol, name, kind, visibility, modality,
            isCompanion, isInner, isData, isExternal, isValue, isExpect, isFun, source
        )

    override fun createConstructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        isInline: Boolean,
        isExpect: Boolean,
        returnType: IrType,
        symbol: IrConstructorSymbol,
        isPrimary: Boolean,
        isExternal: Boolean,
        containerSource: DeserializedContainerSource?
    ): IrConstructor = if (symbol.isBound)
        symbol.owner
    else
        delegate.createConstructor(
            startOffset,
            endOffset,
            origin,
            name,
            visibility,
            isInline,
            isExpect,
            returnType,
            symbol,
            isPrimary,
            isExternal,
            containerSource,
        )

    override fun createEnumEntry(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        symbol: IrEnumEntrySymbol
    ): IrEnumEntry = if (symbol.isBound)
        symbol.owner
    else
        delegate.createEnumEntry(startOffset, endOffset, origin, name, symbol)

    override fun createField(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        symbol: IrFieldSymbol,
        type: IrType,
        isFinal: Boolean,
        isStatic: Boolean,
        isExternal: Boolean
    ): IrField = if (symbol.isBound)
        symbol.owner
    else
        delegate.createField(startOffset, endOffset, origin, name, visibility, symbol, type, isFinal, isStatic, isExternal)

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
        containerSource: DeserializedContainerSource?
    ): IrSimpleFunction = if (symbol.isBound)
        symbol.owner
    else
        delegate.createFunction(
            startOffset, endOffset, origin, symbol, name, visibility, modality, returnType,
            isInline, isExternal, isTailrec, isSuspend, isOperator, isInfix, isExpect, isFakeOverride, containerSource
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
        containerSource: DeserializedContainerSource?
    ): IrProperty = if (symbol.isBound)
        symbol.owner
    else
        delegate.createProperty(
            startOffset, endOffset, origin, symbol, name, visibility, modality,
            isVar, isConst, isLateinit, isDelegated, isExternal, isExpect, isFakeOverride, containerSource
        )

    override fun createTypeAlias(
        startOffset: Int,
        endOffset: Int,
        symbol: IrTypeAliasSymbol,
        name: Name,
        visibility: DescriptorVisibility,
        expandedType: IrType,
        isActual: Boolean,
        origin: IrDeclarationOrigin
    ): IrTypeAlias = if (symbol.isBound)
        symbol.owner
    else
        delegate.createTypeAlias(startOffset, endOffset, symbol, name, visibility, expandedType, isActual, origin)

    override fun createTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrTypeParameterSymbol,
        name: Name,
        index: Int,
        isReified: Boolean,
        variance: Variance
    ): IrTypeParameter = if (symbol.isBound)
        symbol.owner
    else
        delegate.createTypeParameter(startOffset, endOffset, origin, symbol, name, index, isReified, variance)
}