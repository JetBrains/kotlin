/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.factories

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

class DefaultIrDeclarationFactory : IrDeclarationFactory {
    override fun createModuleFragment(descriptor: ModuleDescriptor, irBuiltins: IrBuiltIns): IrModuleFragment = 
        IrModuleFragmentImpl(descriptor, irBuiltins)

    override fun createFile(fileEntry: SourceManager.FileEntry, symbol: IrFileSymbol, fqName: FqName): IrFile = 
        IrFileImpl(fileEntry, symbol, fqName)

    override fun createExternalPackageFragment(symbol: IrExternalPackageFragmentSymbol, fqName: FqName): IrExternalPackageFragment = 
        IrExternalPackageFragmentImpl(symbol, fqName)

    override fun createScript(symbol: IrScriptSymbol, name: Name): IrScript = 
        IrScriptImpl(symbol, name)

    override fun createClass(
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
    ): IrClass =
        IrClassImpl(
            startOffset, endOffset, origin, symbol,
            name, kind, visibility, modality,
            isCompanion, isInner, isData, isExternal, isInline, isExpect
        )

    override fun createSimpleFunction(
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
    ): IrSimpleFunction = 
        IrFunctionImpl(
            startOffset, endOffset, origin, symbol,
            name, visibility, modality, returnType,
            isInline, isExternal, isTailrec, isSuspend, isOperator, isExpect, isFakeOverride
        )

    override fun createConstructor(
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
    ): IrConstructor = 
        IrConstructorImpl(startOffset, endOffset, origin, symbol, name, visibility, returnType, isInline, isExternal, isPrimary, isExpect)

    override fun createProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrPropertySymbol,
        name: Name,
        visibility: Visibility,
        modality: Modality,
        isVar: Boolean,
        isConst: Boolean,
        isLateinit: Boolean,
        isDelegated: Boolean,
        isExternal: Boolean,
        isExpect: Boolean,
        isFakeOverride: Boolean
    ): IrProperty = 
        IrPropertyImpl(
            startOffset, endOffset, origin, symbol,
            name, visibility, modality, isVar,
            isConst, isLateinit, isDelegated, isExternal, isExpect, isFakeOverride
        )

    override fun createField(
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
    ): IrField = 
        IrFieldImpl(startOffset, endOffset, origin, symbol, name, type, visibility, isFinal, isExternal, isStatic, isFakeOverride)

    override fun createLocalDelegatedProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrLocalDelegatedPropertySymbol,
        name: Name,
        type: IrType,
        isVar: Boolean
    ): IrLocalDelegatedProperty = 
        IrLocalDelegatedPropertyImpl(startOffset, endOffset, origin, symbol, name, type, isVar)

    override fun createVariable(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrVariableSymbol,
        name: Name,
        type: IrType,
        isVar: Boolean,
        isConst: Boolean,
        isLateinit: Boolean
    ): IrVariable = 
        IrVariableImpl(startOffset, endOffset, origin, symbol, name, type, isVar, isConst, isLateinit)

    override fun createEnumEntry(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrEnumEntrySymbol,
        name: Name
    ): IrEnumEntry = 
        IrEnumEntryImpl(startOffset, endOffset, origin, symbol, name)

    override fun createAnonymousInitializer(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrAnonymousInitializerSymbol,
        isStatic: Boolean
    ): IrAnonymousInitializer = 
        IrAnonymousInitializerImpl(startOffset, endOffset, origin, symbol, isStatic)

    override fun createTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrTypeParameterSymbol,
        name: Name,
        index: Int,
        isReified: Boolean,
        variance: Variance
    ): IrTypeParameter = 
        IrTypeParameterImpl(startOffset, endOffset, origin, symbol, name, index, isReified, variance)

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
        isNoinline: Boolean
    ): IrValueParameter = 
        IrValueParameterImpl(startOffset, endOffset, origin, symbol, name, index, type, varargElementType, isCrossinline, isNoinline)

    override fun createTypeAlias(
        startOffset: Int,
        endOffset: Int,
        symbol: IrTypeAliasSymbol,
        name: Name,
        visibility: Visibility,
        expandedType: IrType,
        isActual: Boolean,
        origin: IrDeclarationOrigin
    ): IrTypeAlias = 
        IrTypeAliasImpl(startOffset, endOffset, symbol, name, visibility, expandedType, isActual, origin)

    override fun createErrorDeclaration(startOffset: Int, endOffset: Int, descriptor: DeclarationDescriptor): IrErrorDeclaration = 
        IrErrorDeclarationImpl(startOffset, endOffset, descriptor)

    companion object {
        fun createAndRegister(): DefaultIrDeclarationFactory = DefaultIrDeclarationFactory().also {
            IrDeclarationFactory.registerDefaultIrDeclarationFactory(it)
        }
    }
}
