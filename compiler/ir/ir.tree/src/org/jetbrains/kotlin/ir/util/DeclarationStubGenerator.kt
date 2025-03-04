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

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.linkage.IrProvider

@OptIn(ObsoleteDescriptorBasedAPI::class)
abstract class DeclarationStubGenerator(
    val moduleDescriptor: ModuleDescriptor,
    val symbolTable: SymbolTable,
    val irBuiltIns: IrBuiltIns,
    val extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY,
) : IrProvider {
    val lock: IrLock
        get() = symbolTable.lock

    abstract var unboundSymbolGeneration: Boolean
    abstract val typeTranslator: TypeTranslator
    abstract val descriptorFinder: DescriptorByIdSignatureFinder

    abstract fun generateOrGetEmptyExternalPackageFragmentStub(descriptor: PackageFragmentDescriptor): IrExternalPackageFragment

    abstract fun generateOrGetFacadeClass(descriptor: DeclarationDescriptor): IrClass?

    abstract fun generateMemberStub(descriptor: DeclarationDescriptor): IrDeclaration

    abstract fun generatePropertyStub(descriptor: PropertyDescriptor): IrProperty

    abstract fun generateFieldStub(descriptor: PropertyDescriptor): IrField

    abstract fun generateFunctionStub(descriptor: FunctionDescriptor, createPropertyIfNeeded: Boolean = true): IrSimpleFunction

    abstract fun generateConstructorStub(descriptor: ClassConstructorDescriptor): IrConstructor

    abstract fun generateValueParameterStub(descriptor: ValueParameterDescriptor): IrValueParameter

    abstract fun generateClassStub(descriptor: ClassDescriptor): IrClass

    abstract fun generateEnumEntryStub(descriptor: ClassDescriptor): IrEnumEntry

    abstract fun generateOrGetTypeParameterStub(descriptor: TypeParameterDescriptor): IrTypeParameter

    abstract fun generateOrGetScopedTypeParameterStub(descriptor: TypeParameterDescriptor): IrTypeParameter

    abstract fun generateTypeAliasStub(descriptor: TypeAliasDescriptor): IrTypeAlias
}
