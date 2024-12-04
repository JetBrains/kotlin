/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.symbols.*

interface ReferenceSymbolTable {
    @ObsoleteDescriptorBasedAPI
    val descriptorExtension: DescriptorBasedReferenceSymbolTableExtension

    fun referenceClass(signature: IdSignature): IrClassSymbol
    fun referenceConstructor(signature: IdSignature): IrConstructorSymbol
    fun referenceEnumEntry(signature: IdSignature): IrEnumEntrySymbol
    fun referenceField(signature: IdSignature): IrFieldSymbol
    fun referenceProperty(signature: IdSignature): IrPropertySymbol
    fun referenceSimpleFunction(signature: IdSignature): IrSimpleFunctionSymbol
    fun referenceTypeParameter(signature: IdSignature): IrTypeParameterSymbol
    fun referenceTypeAlias(signature: IdSignature): IrTypeAliasSymbol

    fun enterScope(symbol: IrSymbol)
    fun enterScope(owner: IrDeclaration)

    fun leaveScope(symbol: IrSymbol)
    fun leaveScope(owner: IrDeclaration)
}
