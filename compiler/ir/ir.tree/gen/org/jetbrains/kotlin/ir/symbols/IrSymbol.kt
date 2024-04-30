/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.symbols

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker

/**
 * A symbol whose [owner] is either [IrFile] or [IrExternalPackageFragment].
 */
sealed interface IrPackageFragmentSymbol : IrSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: PackageFragmentDescriptor
}

/**
 * A symbol whose [owner] is [IrFile]. Such a symbol is always module-private.
 *
 * [IrFileSymbol] is never actually serialized, but is useful for deserializing private top-level declarations.
 *
 * @see IdSignature.FileSignature
 */
interface IrFileSymbol : IrPackageFragmentSymbol, IrBindableSymbol<PackageFragmentDescriptor, IrFile>

/**
 * A symbol whose [owner] is [IrExternalPackageFragment].
 */
interface IrExternalPackageFragmentSymbol : IrPackageFragmentSymbol, IrBindableSymbol<PackageFragmentDescriptor, IrExternalPackageFragment>

/**
 * A symbol whose [owner] is [IrAnonymousInitializer].
 *
 * It's not very useful on its own, but since [IrAnonymousInitializer] is an [IrDeclaration], and [IrDeclaration]s must have symbols,
 * here we are.
 *
 * This symbol is never public (wrt linkage).
 */
interface IrAnonymousInitializerSymbol : IrBindableSymbol<ClassDescriptor, IrAnonymousInitializer>

/**
 * A symbol whose [owner] is [IrEnumEntry].
 *
 * @see IrGetEnumValue
 */
interface IrEnumEntrySymbol : IrBindableSymbol<ClassDescriptor, IrEnumEntry>, EnumEntrySymbolMarker

/**
 * A symbol whose [owner] is [IrField].
 *
 * @see IrGetField
 * @see IrSetField
 */
interface IrFieldSymbol : IrBindableSymbol<PropertyDescriptor, IrField>, FieldSymbolMarker

/**
 * A symbol whose [owner] is [IrClass], [IrScript] or [IrTypeParameter].
 *
 * @see IrSimpleType
 * @see IrClassReference
 */
sealed interface IrClassifierSymbol : IrSymbol, TypeConstructorMarker {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassifierDescriptor
}

/**
 * A symbol whose [owner] is [IrClass].
 *
 * @see IrClass.sealedSubclasses
 * @see IrCall.superQualifierSymbol
 * @see IrFieldAccessExpression.superQualifierSymbol
 */
interface IrClassSymbol : IrClassifierSymbol, IrBindableSymbol<ClassDescriptor, IrClass>, RegularClassSymbolMarker

/**
 * A symbol whose [owner] is [IrScript].
 */
interface IrScriptSymbol : IrClassifierSymbol, IrBindableSymbol<ScriptDescriptor, IrScript>

/**
 * A symbol whose [owner] is [IrTypeParameter].
 */
interface IrTypeParameterSymbol : IrClassifierSymbol,
    IrBindableSymbol<TypeParameterDescriptor, IrTypeParameter>,
    TypeParameterMarker,
    TypeParameterSymbolMarker

/**
 * A symbol whose [owner] is [IrValueParameter] or [IrVariable].
 *
 * @see IrGetValue
 * @see IrSetValue
 */
sealed interface IrValueSymbol : IrSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ValueDescriptor

    @UnsafeDuringIrConstructionAPI
    override val owner: IrValueDeclaration
}

/**
 * A symbol whose [owner] is [IrValueParameter].
 */
interface IrValueParameterSymbol : IrValueSymbol, IrBindableSymbol<ParameterDescriptor, IrValueParameter>, ValueParameterSymbolMarker

/**
 * A symbol whose [owner] is [IrVariable].
 */
interface IrVariableSymbol : IrValueSymbol, IrBindableSymbol<VariableDescriptor, IrVariable>

/**
 * A symbol whose [owner] is [IrFunction] or [IrReturnableBlock].
 *
 * @see IrReturn
 */
sealed interface IrReturnTargetSymbol : IrSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor

    @UnsafeDuringIrConstructionAPI
    override val owner: IrReturnTarget
}

/**
 * A symbol whose [owner] is [IrConstructor] or [IrSimpleFunction].
 *
 * @see IrFunctionReference
 */
sealed interface IrFunctionSymbol : IrReturnTargetSymbol, FunctionSymbolMarker {
    @UnsafeDuringIrConstructionAPI
    override val owner: IrFunction
}

/**
 * A symbol whose [owner] is [IrConstructor].
 *
 * @see IrConstructorCall
 */
interface IrConstructorSymbol : IrFunctionSymbol, IrBindableSymbol<ClassConstructorDescriptor, IrConstructor>, ConstructorSymbolMarker

/**
 * A symbol whose [owner] is [IrSimpleFunction].
 *
 * @see IrCall
 */
interface IrSimpleFunctionSymbol : IrFunctionSymbol, IrBindableSymbol<FunctionDescriptor, IrSimpleFunction>, SimpleFunctionSymbolMarker

/**
 * A symbol whose [owner] is [IrReturnableBlock].
 */
interface IrReturnableBlockSymbol : IrReturnTargetSymbol, IrBindableSymbol<FunctionDescriptor, IrReturnableBlock>

/**
 * A symbol whose [owner] is [IrProperty].
 */
interface IrPropertySymbol : IrBindableSymbol<PropertyDescriptor, IrProperty>, PropertySymbolMarker

/**
 * A symbol whose [owner] is [IrLocalDelegatedProperty].
 */
interface IrLocalDelegatedPropertySymbol : IrBindableSymbol<VariableDescriptorWithAccessors, IrLocalDelegatedProperty>

/**
 * A symbol whose [owner] is [IrTypeAlias].
 *
 * @see IrTypeAbbreviation
 */
interface IrTypeAliasSymbol : IrBindableSymbol<TypeAliasDescriptor, IrTypeAlias>, TypeAliasSymbolMarker
