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
        name: Name,
        visibility: DescriptorVisibility,
        symbol: IrClassSymbol,
        kind: ClassKind,
        modality: Modality,
        isExternal: Boolean,
        isCompanion: Boolean,
        isInner: Boolean,
        isData: Boolean,
        isValue: Boolean,
        isExpect: Boolean,
        isFun: Boolean,
        hasEnumEntries: Boolean,
        source: SourceElement
    ): IrClass = if (symbol.isBound)
        symbol.owner
    else
        delegate.createClass(
            startOffset,
            endOffset,
            origin,
            name,
            visibility,
            symbol,
            kind,
            modality,
            isExternal,
            isCompanion,
            isInner,
            isData,
            isValue,
            isExpect,
            isFun,
            hasEnumEntries,
            source,
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

    override fun createSimpleFunction(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        isInline: Boolean,
        isExpect: Boolean,
        returnType: IrType,
        modality: Modality,
        symbol: IrSimpleFunctionSymbol,
        isTailrec: Boolean,
        isSuspend: Boolean,
        isOperator: Boolean,
        isInfix: Boolean,
        isExternal: Boolean,
        containerSource: DeserializedContainerSource?,
        isFakeOverride: Boolean
    ): IrSimpleFunction = if (symbol.isBound)
        symbol.owner
    else
        delegate.createSimpleFunction(
            startOffset, endOffset, origin, name, visibility, isInline, isExpect, returnType,
            modality, symbol, isTailrec, isSuspend, isOperator, isInfix, isExternal, containerSource, isFakeOverride
        )

    override fun createProperty(
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
        isExternal: Boolean,
        containerSource: DeserializedContainerSource?,
        isExpect: Boolean,
        isFakeOverride: Boolean
    ): IrProperty = if (symbol.isBound)
        symbol.owner
    else
        delegate.createProperty(
            startOffset,
            endOffset,
            origin,
            name,
            visibility,
            modality,
            symbol,
            isVar,
            isConst,
            isLateinit,
            isDelegated,
            isExternal,
            containerSource,
            isExpect,
            isFakeOverride,
        )

    override fun createTypeAlias(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        symbol: IrTypeAliasSymbol,
        isActual: Boolean,
        expandedType: IrType
    ): IrTypeAlias = if (symbol.isBound)
        symbol.owner
    else
        delegate.createTypeAlias(startOffset, endOffset, origin, name, visibility, symbol, isActual, expandedType)

    override fun createTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        symbol: IrTypeParameterSymbol,
        variance: Variance,
        index: Int,
        isReified: Boolean
    ): IrTypeParameter = if (symbol.isBound)
        symbol.owner
    else
        delegate.createTypeParameter(startOffset, endOffset, origin, name, symbol, variance, index, isReified)
}