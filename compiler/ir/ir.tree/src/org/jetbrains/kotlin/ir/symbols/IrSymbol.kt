/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.symbols

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IrSymbolVisitor
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker

interface IrSymbol {
    val owner: IrSymbolOwner

    @DescriptorBasedIr
    val descriptor: DeclarationDescriptor

    val isBound: Boolean

    val signature: IdSignature

    val isPublicApi: Boolean

    fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R
}

interface IrBindableSymbol<out D : DeclarationDescriptor, B : IrSymbolOwner> : IrSymbol {
    override val owner: B
    override val descriptor: D

    fun bind(owner: B)
}

interface IrPackageFragmentSymbol : IrSymbol {
    override val descriptor: PackageFragmentDescriptor

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitPackageFragmentSymbol(this, data)
}

interface IrFileSymbol :
    IrPackageFragmentSymbol, IrBindableSymbol<PackageFragmentDescriptor, IrFile> {

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitFileSymbol(this, data)
}

interface IrExternalPackageFragmentSymbol :
    IrPackageFragmentSymbol, IrBindableSymbol<PackageFragmentDescriptor, IrExternalPackageFragment> {

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitExternalPackageFragmentSymbol(this, data)
}

interface IrAnonymousInitializerSymbol :
    IrBindableSymbol<ClassDescriptor, IrAnonymousInitializer> {

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitAnonymousInitializerSymbol(this, data)
}

interface IrEnumEntrySymbol :
    IrBindableSymbol<ClassDescriptor, IrEnumEntry> {

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitEnumEntrySymbol(this, data)
}

interface IrFieldSymbol :
    IrBindableSymbol<PropertyDescriptor, IrField> {

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitFieldSymbol(this, data)
}

interface IrClassifierSymbol :
    IrSymbol, TypeConstructorMarker {

    override val descriptor: ClassifierDescriptor

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitClassifierSymbol(this, data)
}

interface IrClassSymbol :
    IrClassifierSymbol, IrBindableSymbol<ClassDescriptor, IrClass> {

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitClassSymbol(this, data)
}

interface IrScriptSymbol :
    IrSymbol, IrBindableSymbol<ScriptDescriptor, IrScript> {

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitSymbol(this, data)
}

interface IrTypeParameterSymbol :
    IrClassifierSymbol, IrBindableSymbol<TypeParameterDescriptor, IrTypeParameter>, TypeParameterMarker {

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitTypeParameterSymbol(this, data)
}

interface IrValueSymbol :
    IrSymbol {

    override val descriptor: ValueDescriptor
    override val owner: IrValueDeclaration

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitValueSymbol(this, data)
}

interface IrValueParameterSymbol :
    IrValueSymbol, IrBindableSymbol<ParameterDescriptor, IrValueParameter> {

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitValueParameterSymbol(this, data)
}

interface IrVariableSymbol :
    IrValueSymbol, IrBindableSymbol<VariableDescriptor, IrVariable> {

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitVariableSymbol(this, data)
}

interface IrReturnTargetSymbol :
    IrSymbol {

    override val descriptor: FunctionDescriptor
    override val owner: IrReturnTarget

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitReturnTargetSymbol(this, data)
}

interface IrFunctionSymbol :
    IrReturnTargetSymbol {

    override val owner: IrFunction

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitFunctionSymbol(this, data)
}

interface IrConstructorSymbol :
    IrFunctionSymbol, IrBindableSymbol<ClassConstructorDescriptor, IrConstructor> {

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitConstructorSymbol(this, data)
}
interface IrSimpleFunctionSymbol :
    IrFunctionSymbol, IrBindableSymbol<FunctionDescriptor, IrSimpleFunction> {

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitSimpleFunctionSymbol(this, data)
}

interface IrReturnableBlockSymbol :
    IrReturnTargetSymbol, IrBindableSymbol<FunctionDescriptor, IrReturnableBlock> {

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitReturnableBlockSymbol(this, data)
}

interface IrPropertySymbol :
    IrBindableSymbol<PropertyDescriptor, IrProperty> {

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitPropertySymbol(this, data)
}

interface IrLocalDelegatedPropertySymbol :
    IrBindableSymbol<VariableDescriptorWithAccessors, IrLocalDelegatedProperty> {

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitLocalDelegatedPropertySymbol(this, data)
}

interface IrTypeAliasSymbol :
    IrBindableSymbol<TypeAliasDescriptor, IrTypeAlias> {

    override fun <D, R> accept(visitor: IrSymbolVisitor<R, D>, data: D): R =
        visitor.visitTypeAliasSymbol(this, data)
}