/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.factories

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.types.Variance

interface IrDeclarationFactory {
    fun createModuleFragment(
        descriptor: ModuleDescriptor,
        irBuiltins: IrBuiltIns
    ): IrModuleFragment

    fun createFile(
        fileEntry: SourceManager.FileEntry,
        symbol: IrFileSymbol,
        fqName: FqName
    ): IrFile

    fun createExternalPackageFragment(
        symbol: IrExternalPackageFragmentSymbol,
        fqName: FqName
    ): IrExternalPackageFragment

    fun createScript(
        symbol: IrScriptSymbol,
        name: Name
    ): IrScript

    fun createClass(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrClassSymbol,
        name: Name,
        kind: ClassKind,
        visibility: Visibility,
        modality: Modality,
        isCompanion: Boolean,
        isInner: Boolean,
        isData: Boolean,
        isExternal: Boolean,
        isInline: Boolean,
        isExpect: Boolean
    ): IrClass

    fun createSimpleFunction(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrSimpleFunctionSymbol,
        name: Name,
        visibility: Visibility,
        modality: Modality,
        returnType: IrType,
        isInline: Boolean,
        isExternal: Boolean,
        isTailrec: Boolean,
        isSuspend: Boolean,
        isOperator: Boolean,
        isExpect: Boolean,
        isFakeOverride: Boolean
    ): IrSimpleFunction

    fun createConstructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrConstructorSymbol,
        name: Name,
        visibility: Visibility,
        returnType: IrType,
        isInline: Boolean,
        isExternal: Boolean,
        isPrimary: Boolean,
        isExpect: Boolean
    ): IrConstructor

    fun createProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrPropertySymbol,
        name: Name = symbol.descriptor.name,
        visibility: Visibility = symbol.descriptor.visibility,
        modality: Modality = symbol.descriptor.modality,
        isVar: Boolean = symbol.descriptor.isVar,
        isConst: Boolean = symbol.descriptor.isConst,
        isLateinit: Boolean = symbol.descriptor.isLateInit,
        isDelegated: Boolean = @Suppress("DEPRECATION") symbol.descriptor.isDelegated,
        isExternal: Boolean = symbol.descriptor.isEffectivelyExternal(),
        isExpect: Boolean = symbol.descriptor.isExpect,
        isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE
    ): IrProperty

    fun createField(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrFieldSymbol,
        name: Name,
        type: IrType,
        visibility: Visibility,
        isFinal: Boolean,
        isExternal: Boolean,
        isStatic: Boolean,
        isFakeOverride: Boolean
    ): IrField

    fun createLocalDelegatedProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrLocalDelegatedPropertySymbol,
        name: Name,
        type: IrType,
        isVar: Boolean
    ): IrLocalDelegatedProperty

    fun createVariable(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrVariableSymbol,
        name: Name,
        type: IrType,
        isVar: Boolean,
        isConst: Boolean,
        isLateinit: Boolean
    ): IrVariable

    fun createEnumEntry(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrEnumEntrySymbol,
        name: Name
    ): IrEnumEntry

    fun createAnonymousInitializer(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrAnonymousInitializerSymbol,
        isStatic: Boolean = false
    ): IrAnonymousInitializer

    fun createTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrTypeParameterSymbol,
        name: Name,
        index: Int,
        isReified: Boolean,
        variance: Variance
    ): IrTypeParameter

    fun createValueParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrValueParameterSymbol,
        name: Name,
        index: Int,
        type: IrType,
        varargElementType: IrType?,
        isCrossinline: Boolean,
        isNoinline: Boolean
    ): IrValueParameter

    fun createTypeAlias(
        startOffset: Int,
        endOffset: Int,
        symbol: IrTypeAliasSymbol,
        name: Name,
        visibility: Visibility,
        expandedType: IrType,
        isActual: Boolean,
        origin: IrDeclarationOrigin
    ): IrTypeAlias

    fun createErrorDeclaration(
        startOffset: Int,
        endOffset: Int,
        descriptor: DeclarationDescriptor
    ): IrErrorDeclaration

    companion object {
        private var factory: IrDeclarationFactory? = null

        @Deprecated("Use it carefully! Prefer to provide factory explicitly!")
        val DEFAULT: IrDeclarationFactory
            get() = factory ?: error("No registered default IrDeclarationFactory")

        @Deprecated("For temparary use")
        val TMP: IrDeclarationFactory
            get() = DEFAULT

        @Deprecated("Use it carefully!")
        fun registerDefaultIrDeclarationFactory(newFactory: IrDeclarationFactory) {
            factory?.let { error("${it::class.simpleName} is already registered as default IrDeclarationFactory") }

            factory = newFactory
        }

        fun resetDefaultIrDeclarationFactory() {
            factory = null
        }
    }
}
