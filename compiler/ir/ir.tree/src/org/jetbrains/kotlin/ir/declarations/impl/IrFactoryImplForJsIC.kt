/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.Variance
import java.util.*

class IrFactoryImplForJsIC(override val stageController: StageController) : AbstractIrFactoryImpl(), IdSignatureRetriever {
    private val declarationToSignature = WeakHashMap<IrDeclaration, IdSignature>()

    private fun <T : IrDeclaration> T.register(): T {
        val parentSig = stageController.currentDeclaration?.let { declarationSignature(it) } ?: return this

        stageController.createSignature(parentSig)?.let { declarationToSignature[this] = it }

        return this
    }

    override fun declarationSignature(declaration: IrDeclaration): IdSignature? {
        return declarationToSignature[declaration] ?: declaration.symbol.signature ?: declaration.symbol.privateSignature
    }

    override fun createAnonymousInitializer(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrAnonymousInitializerSymbol,
        isStatic: Boolean
    ): IrAnonymousInitializer {
        return super.createAnonymousInitializer(
            startOffset,
            endOffset,
            origin,
            symbol,
            isStatic,
        ).register()
    }

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
    ): IrClass {
        return super.createClass(
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
        ).register()
    }

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
    ): IrConstructor {
        return super.createConstructor(
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
        ).register()
    }

    override fun createEnumEntry(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        symbol: IrEnumEntrySymbol
    ): IrEnumEntry {
        return super.createEnumEntry(
            startOffset,
            endOffset,
            origin,
            name,
            symbol,
        ).register()
    }

    override fun createErrorDeclaration(startOffset: Int, endOffset: Int, descriptor: DeclarationDescriptor?): IrErrorDeclaration {
        return super.createErrorDeclaration(startOffset, endOffset, descriptor)
    }

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
    ): IrField {
        return super.createField(
            startOffset,
            endOffset,
            origin,
            name,
            visibility,
            symbol,
            type,
            isFinal,
            isStatic,
            isExternal,
        ).register()
    }

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
    ): IrSimpleFunction {
        return super.createSimpleFunction(
            startOffset,
            endOffset,
            origin,
            name,
            visibility,
            isInline,
            isExpect,
            returnType,
            modality,
            symbol,
            isTailrec,
            isSuspend,
            isOperator,
            isInfix,
            isExternal,
            containerSource,
            isFakeOverride,
        ).register()
    }

    override fun createFunctionWithLateBinding(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        isInline: Boolean,
        isExpect: Boolean,
        returnType: IrType,
        modality: Modality,
        isTailrec: Boolean,
        isSuspend: Boolean,
        isOperator: Boolean,
        isInfix: Boolean,
        isExternal: Boolean,
        isFakeOverride: Boolean,
    ): IrFunctionWithLateBinding {
        return super.createFunctionWithLateBinding(
            startOffset,
            endOffset,
            origin,
            name,
            visibility,
            isInline,
            isExpect,
            returnType,
            modality,
            isTailrec,
            isSuspend,
            isOperator,
            isInfix,
            isExternal,
            isFakeOverride,
        ).register()
    }

    override fun createLocalDelegatedProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        symbol: IrLocalDelegatedPropertySymbol,
        type: IrType,
        isVar: Boolean
    ): IrLocalDelegatedProperty {
        return super.createLocalDelegatedProperty(
            startOffset,
            endOffset,
            origin,
            name,
            symbol,
            type,
            isVar,
        ).register()
    }

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
    ): IrProperty {
        return super.createProperty(
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
        ).register()
    }

    override fun createPropertyWithLateBinding(
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
        isFakeOverride: Boolean,
    ): IrPropertyWithLateBinding {
        return super.createPropertyWithLateBinding(
            startOffset,
            endOffset,
            origin,
            name,
            visibility,
            modality,
            isVar,
            isConst,
            isLateinit,
            isDelegated,
            isExternal,
            isExpect,
            isFakeOverride,
        ).register()
    }

    override fun createTypeAlias(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        symbol: IrTypeAliasSymbol,
        isActual: Boolean,
        expandedType: IrType
    ): IrTypeAlias {
        return super.createTypeAlias(
            startOffset,
            endOffset,
            origin,
            name,
            visibility,
            symbol,
            isActual,
            expandedType,
        ).register()
    }

    override fun createTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        symbol: IrTypeParameterSymbol,
        variance: Variance,
        index: Int,
        isReified: Boolean
    ): IrTypeParameter {
        return super.createTypeParameter(
            startOffset,
            endOffset,
            origin,
            name,
            symbol,
            variance,
            index,
            isReified,
        ).register()
    }

    override fun createValueParameter(
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
        isHidden: Boolean
    ): IrValueParameter {
        return super.createValueParameter(
            startOffset,
            endOffset,
            origin,
            name,
            type,
            isAssignable,
            symbol,
            index,
            varargElementType,
            isCrossinline,
            isNoinline,
            isHidden,
        ).register()
    }
}
