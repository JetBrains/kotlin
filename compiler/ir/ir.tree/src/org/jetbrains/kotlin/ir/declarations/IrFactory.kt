/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.Variance

@OptIn(IrImplementationDetail::class)
open class IrFactory(
    val stageController: StageController,
) {
    protected open fun <T : IrDeclaration> T.declarationCreated(): T {
        return this
    }

    fun createAnonymousInitializer(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrAnonymousInitializerSymbol,
        isStatic: Boolean = false,
    ): IrAnonymousInitializer =
        IrAnonymousInitializerImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            symbol = symbol,
            isStatic = isStatic,
            factory = this
        ).declarationCreated()

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
    ): IrClass =
        IrClassImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            symbol = symbol,
            name = name,
            kind = kind,
            visibility = visibility,
            modality = modality,
            source = source,
            factory = this
        ).declarationCreated().apply {
            this.isCompanion = isCompanion
            this.isInner = isInner
            this.isData = isData
            this.isExternal = isExternal
            this.isValue = isValue
            this.isExpect = isExpect
            this.isFun = isFun
            this.hasEnumEntries = hasEnumEntries
        }

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
    ): IrConstructor =
        IrConstructorImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            symbol = symbol,
            name = name,
            visibility = visibility,
            isInline = isInline,
            isExternal = isExternal,
            isPrimary = isPrimary,
            isExpect = isExpect,
            containerSource = containerSource,
            factory = this
        ).declarationCreated().apply {
            if (returnType != null) {
                this.returnType = returnType
            }
        }

    fun createEnumEntry(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        symbol: IrEnumEntrySymbol,
    ): IrEnumEntry =
        IrEnumEntryImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            symbol = symbol,
            name = name,
            factory = this
        ).declarationCreated()

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun createErrorDeclaration(
        startOffset: Int,
        endOffset: Int,
        descriptor: DeclarationDescriptor? = null,
    ): IrErrorDeclaration =
        IrErrorDeclarationImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            factory = this,
            origin = IrDeclarationOrigin.DEFINED,
        ).declarationCreated().apply {
            this.descriptor = descriptor ?: this.toIrBasedDescriptor()
        }

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
    ): IrField =
        IrFieldImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            symbol = symbol,
            name = name,
            type = type,
            visibility = visibility,
            isFinal = isFinal,
            isExternal = isExternal,
            isStatic = isStatic,
            factory = this
        ).declarationCreated()

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
    ): IrSimpleFunction =
        IrFunctionImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            symbol = symbol,
            name = name,
            visibility = visibility,
            modality = modality,
            isInline = isInline,
            isExternal = isExternal,
            isTailrec = isTailrec,
            isSuspend = isSuspend,
            isOperator = isOperator,
            isInfix = isInfix,
            isExpect = isExpect,
            isFakeOverride = isFakeOverride,
            containerSource = containerSource,
            factory = this
        ).declarationCreated().apply {
            if (returnType != null) {
                this.returnType = returnType
            }
        }

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
    ): IrFunctionWithLateBinding =
        IrFunctionWithLateBindingImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            name = name,
            visibility = visibility,
            modality = modality,
            isInline = isInline,
            isExternal = isExternal,
            isTailrec = isTailrec,
            isSuspend = isSuspend,
            isOperator = isOperator,
            isInfix = isInfix,
            isExpect = isExpect,
            isFakeOverride = isFakeOverride,
            factory = this
        ).declarationCreated().apply {
            if (returnType != null) {
                this.returnType = returnType
            }
        }

    fun createLocalDelegatedProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        symbol: IrLocalDelegatedPropertySymbol,
        type: IrType,
        isVar: Boolean,
    ): IrLocalDelegatedProperty =
        IrLocalDelegatedPropertyImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            symbol = symbol,
            name = name,
            type = type,
            isVar = isVar,
            factory = this
        ).declarationCreated()

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
    ): IrProperty =
        IrPropertyImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            symbol = symbol,
            name = name,
            visibility = visibility,
            modality = modality,
            isVar = isVar,
            isConst = isConst,
            isLateinit = isLateinit,
            isDelegated = isDelegated,
            isExternal = isExternal,
            isExpect = isExpect,
            isFakeOverride = isFakeOverride,
            containerSource = containerSource,
            factory = this
        ).declarationCreated()

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
    ): IrPropertyWithLateBinding =
        IrPropertyWithLateBindingImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            name = name,
            visibility = visibility,
            modality = modality,
            isVar = isVar,
            isConst = isConst,
            isLateinit = isLateinit,
            isDelegated = isDelegated,
            isExternal = isExternal,
            isExpect = isExpect,
            isFakeOverride = isFakeOverride,
            factory = this
        ).declarationCreated()

    fun createTypeAlias(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        symbol: IrTypeAliasSymbol,
        isActual: Boolean,
        expandedType: IrType,
    ): IrTypeAlias =
        IrTypeAliasImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            symbol = symbol,
            name = name,
            visibility = visibility,
            expandedType = expandedType,
            isActual = isActual,
            origin = origin,
            factory = this
        ).declarationCreated()

    fun createTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        symbol: IrTypeParameterSymbol,
        variance: Variance,
        index: Int,
        isReified: Boolean,
    ): IrTypeParameter =
        IrTypeParameterImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            symbol = symbol,
            name = name,
            index = index,
            isReified = isReified,
            variance = variance,
            factory = this
        ).declarationCreated()

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
    ): IrValueParameter =
        IrValueParameterImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            symbol = symbol,
            name = name,
            index = index,
            type = type,
            varargElementType = varargElementType,
            isCrossinline = isCrossinline,
            isNoinline = isNoinline,
            isHidden = isHidden,
            isAssignable = isAssignable,
            factory = this
        ).declarationCreated()

    fun createExpressionBody(
        startOffset: Int,
        endOffset: Int,
        expression: IrExpression,
    ): IrExpressionBody =
        IrExpressionBodyImpl(
            constructorIndicator = null,
            startOffset = startOffset,
            endOffset = endOffset,
            expression = expression
        )

    fun createBlockBody(
        startOffset: Int,
        endOffset: Int,
    ): IrBlockBody =
        IrBlockBodyImpl(
            constructorIndicator = null,
            startOffset = startOffset,
            endOffset = endOffset
        )
}