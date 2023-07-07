/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.symbols.*

interface ReferenceSymbolTable {
    @ObsoleteDescriptorBasedAPI
    fun referenceClass(descriptor: ClassDescriptor): IrClassSymbol
    fun referenceClass(signature: IdSignature): IrClassSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceScript(descriptor: ScriptDescriptor): IrScriptSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol
    fun referenceConstructor(signature: IdSignature): IrConstructorSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceEnumEntry(descriptor: ClassDescriptor): IrEnumEntrySymbol
    fun referenceEnumEntry(signature: IdSignature): IrEnumEntrySymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceField(descriptor: PropertyDescriptor): IrFieldSymbol
    fun referenceField(signature: IdSignature): IrFieldSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceProperty(descriptor: PropertyDescriptor): IrPropertySymbol
    fun referenceProperty(signature: IdSignature): IrPropertySymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceSimpleFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol
    fun referenceSimpleFunction(signature: IdSignature): IrSimpleFunctionSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceDeclaredFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceValueParameter(descriptor: ParameterDescriptor): IrValueParameterSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceTypeParameter(classifier: TypeParameterDescriptor): IrTypeParameterSymbol
    fun referenceTypeParameter(signature: IdSignature): IrTypeParameterSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceScopedTypeParameter(classifier: TypeParameterDescriptor): IrTypeParameterSymbol

    @ObsoleteDescriptorBasedAPI
    fun referenceTypeAlias(descriptor: TypeAliasDescriptor): IrTypeAliasSymbol
    fun referenceTypeAlias(signature: IdSignature): IrTypeAliasSymbol

    fun enterScope(symbol: IrSymbol)
    fun enterScope(owner: IrDeclaration)

    fun leaveScope(symbol: IrSymbol)
    fun leaveScope(owner: IrDeclaration)
}
