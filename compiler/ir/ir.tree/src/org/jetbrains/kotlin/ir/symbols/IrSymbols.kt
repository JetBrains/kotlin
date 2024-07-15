/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

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
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.packageFragmentSymbol]
 */
sealed interface IrPackageFragmentSymbol : IrSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: PackageFragmentDescriptor
}

/**
 * A symbol whose [owner] is [IrFile].
 *
 * Such a symbol is always module-private.
 *
 * [IrFileSymbol] is never actually serialized, but is useful for deserializing private top-level declarations.
 *
 * See also: [IdSignature.FileSignature].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.fileSymbol]
 */
interface IrFileSymbol : IrPackageFragmentSymbol, IrBindableSymbol<PackageFragmentDescriptor, IrFile>

/**
 * A symbol whose [owner] is [IrExternalPackageFragment].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.externalPackageFragmentSymbol]
 */
interface IrExternalPackageFragmentSymbol : IrPackageFragmentSymbol, IrBindableSymbol<PackageFragmentDescriptor, IrExternalPackageFragment>

/**
 * A symbol whose [owner] is [IrAnonymousInitializer].
 *
 * It's not very useful on its own, but since [IrAnonymousInitializer] is an [IrDeclaration], and [IrDeclaration]s must have symbols,
 * here we are.
 *
 * This symbol is never public (wrt linkage).
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.anonymousInitializerSymbol]
 */
interface IrAnonymousInitializerSymbol : IrBindableSymbol<ClassDescriptor, IrAnonymousInitializer>

/**
 * A symbol whose [owner] is [IrEnumEntry].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.enumEntrySymbol]
 *
 * @see IrGetEnumValue.symbol
 */
interface IrEnumEntrySymbol : IrBindableSymbol<ClassDescriptor, IrEnumEntry>, EnumEntrySymbolMarker

/**
 * A symbol whose [owner] is [IrField].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.fieldSymbol]
 *
 * @see IrPropertyReference.field
 * @see IrGetField.symbol
 * @see IrSetField.symbol
 */
interface IrFieldSymbol : IrBindableSymbol<PropertyDescriptor, IrField>, FieldSymbolMarker

/**
 * A symbol whose [owner] is [IrClass], [IrScript] or [IrTypeParameter].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.classifierSymbol]
 *
 * @see IrClassReference.symbol
 * @see IrSimpleType.classifier
 */
sealed interface IrClassifierSymbol : IrSymbol, TypeConstructorMarker {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassifierDescriptor
}

/**
 * A symbol whose [owner] is [IrClass].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.classSymbol]
 *
 * @see IrClass.sealedSubclasses
 * @see IrScript.targetClass
 * @see IrGetObjectValue.symbol
 * @see IrCall.superQualifierSymbol
 * @see IrInstanceInitializerCall.classSymbol
 */
interface IrClassSymbol : IrClassifierSymbol, IrBindableSymbol<ClassDescriptor, IrClass>, RegularClassSymbolMarker

/**
 * A symbol whose [owner] is [IrScript].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.scriptSymbol]
 *
 * @see IrScript.importedScripts
 * @see IrScript.earlierScripts
 */
interface IrScriptSymbol : IrClassifierSymbol, IrBindableSymbol<ScriptDescriptor, IrScript>

/**
 * A symbol whose [owner] is [IrTypeParameter].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.typeParameterSymbol]
 */
interface IrTypeParameterSymbol : IrClassifierSymbol, IrBindableSymbol<TypeParameterDescriptor, IrTypeParameter>, TypeParameterMarker, TypeParameterSymbolMarker

/**
 * A symbol whose [owner] is either [IrValueParameter] or [IrVariable].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.valueSymbol]
 *
 * @see IrGetValue.symbol
 * @see IrSetValue.symbol
 */
sealed interface IrValueSymbol : IrSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ValueDescriptor

    @UnsafeDuringIrConstructionAPI
    override val owner: IrValueDeclaration
}

/**
 * A symbol whose [owner] is [IrValueParameter].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.valueParameterSymbol]
 */
interface IrValueParameterSymbol : IrValueSymbol, IrBindableSymbol<ParameterDescriptor, IrValueParameter>, ValueParameterSymbolMarker

/**
 * A symbol whose [owner] is [IrVariable].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.variableSymbol]
 *
 * @see IrLocalDelegatedPropertyReference.delegate
 */
interface IrVariableSymbol : IrValueSymbol, IrBindableSymbol<VariableDescriptor, IrVariable>

/**
 * A symbol whose [owner] is either [IrFunction] or [IrReturnableBlock].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.returnTargetSymbol]
 *
 * @see IrReturn.returnTargetSymbol
 */
sealed interface IrReturnTargetSymbol : IrSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor

    @UnsafeDuringIrConstructionAPI
    override val owner: IrReturnTarget
}

/**
 * A symbol whose [owner] is either [IrConstructor] or [IrSimpleFunction].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.functionSymbol]
 *
 * @see IrRawFunctionReference.symbol
 * @see IrFunctionReference.symbol
 */
sealed interface IrFunctionSymbol : IrReturnTargetSymbol, FunctionSymbolMarker {
    @UnsafeDuringIrConstructionAPI
    override val owner: IrFunction
}

/**
 * A symbol whose [owner] is [IrConstructor].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.constructorSymbol]
 *
 * @see IrConstructorCall.symbol
 * @see IrConstantObject.constructor
 * @see IrDelegatingConstructorCall.symbol
 * @see IrEnumConstructorCall.symbol
 */
interface IrConstructorSymbol : IrFunctionSymbol, IrBindableSymbol<ClassConstructorDescriptor, IrConstructor>, ConstructorSymbolMarker

/**
 * A symbol whose [owner] is [IrSimpleFunction].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.simpleFunctionSymbol]
 *
 * @see IrCall.symbol
 * @see IrPropertyReference.getter
 * @see IrPropertyReference.setter
 * @see IrLocalDelegatedPropertyReference.getter
 * @see IrLocalDelegatedPropertyReference.setter
 */
interface IrSimpleFunctionSymbol : IrFunctionSymbol, IrBindableSymbol<FunctionDescriptor, IrSimpleFunction>, SimpleFunctionSymbolMarker

/**
 * A symbol whose [owner] is [IrReturnableBlock].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.returnableBlockSymbol]
 */
interface IrReturnableBlockSymbol : IrReturnTargetSymbol, IrBindableSymbol<FunctionDescriptor, IrReturnableBlock>

/**
 * A symbol whose [owner] is [IrProperty].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.propertySymbol]
 *
 * @see IrFunctionWithLateBinding.correspondingPropertySymbol
 * @see IrField.correspondingPropertySymbol
 * @see IrScript.providedProperties
 * @see IrScript.resultProperty
 * @see IrSimpleFunction.correspondingPropertySymbol
 * @see IrPropertyReference.symbol
 */
interface IrPropertySymbol : IrBindableSymbol<PropertyDescriptor, IrProperty>, PropertySymbolMarker

/**
 * A symbol whose [owner] is [IrLocalDelegatedProperty].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.localDelegatedPropertySymbol]
 *
 * @see IrLocalDelegatedPropertyReference.symbol
 */
interface IrLocalDelegatedPropertySymbol : IrBindableSymbol<VariableDescriptorWithAccessors, IrLocalDelegatedProperty>

/**
 * A symbol whose [owner] is [IrTypeAlias].
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrSymbolTree.typeAliasSymbol]
 *
 * @see IrTypeAbbreviation.typeAlias
 */
interface IrTypeAliasSymbol : IrBindableSymbol<TypeAliasDescriptor, IrTypeAlias>, TypeAliasSymbolMarker
