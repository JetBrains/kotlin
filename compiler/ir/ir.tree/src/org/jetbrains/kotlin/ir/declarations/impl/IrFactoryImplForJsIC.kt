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
    ): IrClass {
        return super.createClass(
            startOffset,
            endOffset,
            origin,
            symbol,
            name,
            kind,
            visibility,
            modality,
            isCompanion,
            isInner,
            isData,
            isExternal,
            isValue,
            isExpect,
            isFun,
            source,
        ).register()
    }

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
        containerSource: DeserializedContainerSource?
    ): IrConstructor {
        return super.createConstructor(
            startOffset,
            endOffset,
            origin,
            symbol,
            name,
            visibility,
            returnType,
            isInline,
            isExternal,
            isPrimary,
            isExpect,
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
        symbol: IrFieldSymbol,
        name: Name,
        type: IrType,
        visibility: DescriptorVisibility,
        isFinal: Boolean,
        isExternal: Boolean,
        isStatic: Boolean
    ): IrField {
        return super.createField(
            startOffset,
            endOffset,
            origin,
            symbol,
            name,
            type,
            visibility,
            isFinal,
            isExternal,
            isStatic,
        ).register()
    }

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
    ): IrSimpleFunction {
        return super.createFunction(
            startOffset,
            endOffset,
            origin,
            symbol,
            name,
            visibility,
            modality,
            returnType,
            isInline,
            isExternal,
            isTailrec,
            isSuspend,
            isOperator,
            isInfix,
            isExpect,
            isFakeOverride,
            containerSource,
        ).register()
    }

    override fun createFunctionWithLateBinding(
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
        isExpect: Boolean
    ): IrSimpleFunction {
        return super.createFunctionWithLateBinding(
            startOffset,
            endOffset,
            origin,
            name,
            visibility,
            modality,
            returnType,
            isInline,
            isExternal,
            isTailrec,
            isSuspend,
            isOperator,
            isInfix,
            isExpect,
        ).register()
    }

    override fun createLocalDelegatedProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrLocalDelegatedPropertySymbol,
        name: Name,
        type: IrType,
        isVar: Boolean
    ): IrLocalDelegatedProperty {
        return super.createLocalDelegatedProperty(
            startOffset,
            endOffset,
            origin,
            symbol,
            name,
            type,
            isVar,
        ).register()
    }

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
    ): IrProperty {
        return super.createProperty(
            startOffset,
            endOffset,
            origin,
            symbol,
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
            containerSource,
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
        isExpect: Boolean
    ): IrProperty {
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
        ).register()
    }

    override fun createTypeAlias(
        startOffset: Int,
        endOffset: Int,
        symbol: IrTypeAliasSymbol,
        name: Name,
        visibility: DescriptorVisibility,
        expandedType: IrType,
        isActual: Boolean,
        origin: IrDeclarationOrigin
    ): IrTypeAlias {
        return super.createTypeAlias(
            startOffset,
            endOffset,
            symbol,
            name,
            visibility,
            expandedType,
            isActual,
            origin,
        ).register()
    }

    override fun createTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrTypeParameterSymbol,
        name: Name,
        index: Int,
        isReified: Boolean,
        variance: Variance
    ): IrTypeParameter {
        return super.createTypeParameter(
            startOffset,
            endOffset,
            origin,
            symbol,
            name,
            index,
            isReified,
            variance,
        ).register()
    }

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
    ): IrValueParameter {
        return super.createValueParameter(
            startOffset,
            endOffset,
            origin,
            symbol,
            name,
            index,
            type,
            varargElementType,
            isCrossinline,
            isNoinline,
            isHidden,
            isAssignable,
        ).register()
    }
}
