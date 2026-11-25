/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrScriptImpl
import org.jetbrains.kotlin.ir.descriptors.IrBasedClassDescriptor
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.utils.threadLocal

@ObsoleteDescriptorBasedAPI
typealias DescriptorBasedReferenceSymbolTableExtension = ReferenceSymbolTableExtension<
        ClassDescriptor, TypeAliasDescriptor, ScriptDescriptor, FunctionDescriptor, ClassConstructorDescriptor,
        PropertyDescriptor, ParameterDescriptor, TypeParameterDescriptor
        >

@ObsoleteDescriptorBasedAPI
open class DescriptorSymbolTableExtension(table: SymbolTable) : SymbolTableExtension<
        DeclarationDescriptor, ClassDescriptor, TypeAliasDescriptor, ScriptDescriptor, FunctionDescriptor,
        ClassConstructorDescriptor, PropertyDescriptor, ParameterDescriptor, TypeParameterDescriptor>(table)
{
    private val irFactory: IrFactory
        get() = table.irFactory

    private val nameProvider: NameProvider
        get() = table.nameProvider

    private val signatureComposer: IdSignatureComposer
        get() = table.signaturer!!

    private val valueParameterSlice: SymbolTableSlice.Scoped<ParameterDescriptor, IrValueParameter, IrValueParameterSymbol> by threadLocal {
        SymbolTableSlice.Scoped(lock)
    }

    private val variableSlice: SymbolTableSlice.Scoped<VariableDescriptor, IrVariable, IrVariableSymbol> by threadLocal {
        SymbolTableSlice.Scoped(lock)
    }

    private val localDelegatedPropertySlice: SymbolTableSlice.Scoped<VariableDescriptorWithAccessors, IrLocalDelegatedProperty, IrLocalDelegatedPropertySymbol> by threadLocal {
        SymbolTableSlice.Scoped(lock)
    }

    override fun MutableList<SymbolTableSlice.Scoped<*, *, *>>.initializeScopedSlices() {
        add(valueParameterSlice)
        add(variableSlice)
        add(localDelegatedPropertySlice)
    }

    // ------------------------------------ signature ------------------------------------

    override fun calculateSignature(declaration: DeclarationDescriptor): IdSignature? {
        return signatureComposer.composeSignature(declaration)
    }

    override fun calculateEnumEntrySignature(declaration: ClassDescriptor): IdSignature? {
        return signatureComposer.composeEnumEntrySignature(declaration)
    }

    override fun calculateFieldSignature(declaration: PropertyDescriptor): IdSignature? {
        return signatureComposer.composeFieldSignature(declaration)
    }

    // ------------------------------------ script ------------------------------------

    override fun defaultScriptFactory(startOffset: Int, endOffset: Int, script: ScriptDescriptor, symbol: IrScriptSymbol): IrScript {
        return IrScriptImpl(symbol, nameProvider.nameForDeclaration(script), irFactory, startOffset, endOffset)
    }

    override fun createScriptSymbol(declaration: ScriptDescriptor, signature: IdSignature?): IrScriptSymbol {
        return IrScriptSymbolImpl()
    }

    // ------------------------------------ class ------------------------------------

    override fun referenceClass(declaration: ClassDescriptor): IrClassSymbol {
        return if (declaration is IrBasedClassDescriptor) {
            declaration.owner.symbol
        } else {
            super.referenceClass(declaration)
        }
    }

    override fun createPublicClassSymbol(declaration: ClassDescriptor, signature: IdSignature): IrClassSymbol {
        return IrClassSymbolImpl(signature)
    }

    override fun createPrivateClassSymbol(descriptor: ClassDescriptor): IrClassSymbol {
        return IrClassSymbolImpl()
    }

    // ------------------------------------ constructor ------------------------------------

    override fun createPublicConstructorSymbol(declaration: ClassConstructorDescriptor, signature: IdSignature): IrConstructorSymbol {
        return IrConstructorSymbolImpl(signature)
    }

    override fun createPrivateConstructorSymbol(declaration: ClassConstructorDescriptor): IrConstructorSymbol {
        return IrConstructorSymbolImpl()
    }

    // ------------------------------------ enum entry ------------------------------------

    override fun createPublicEnumEntrySymbol(declaration: ClassDescriptor, signature: IdSignature): IrEnumEntrySymbol {
        return IrEnumEntrySymbolImpl(signature)
    }

    override fun createPrivateEnumEntrySymbol(declaration: ClassDescriptor): IrEnumEntrySymbol {
        return IrEnumEntrySymbolImpl()
    }

    // ------------------------------------ field ------------------------------------

    override fun createPublicFieldSymbol(declaration: PropertyDescriptor, signature: IdSignature): IrFieldSymbol {
        return IrFieldSymbolImpl(signature)
    }

    override fun createPrivateFieldSymbol(declaration: PropertyDescriptor): IrFieldSymbol {
        return IrFieldSymbolImpl()
    }

    // ------------------------------------ property ------------------------------------

    override fun createPublicPropertySymbol(declaration: PropertyDescriptor, signature: IdSignature): IrPropertySymbol {
        return IrPropertySymbolImpl(signature)
    }

    override fun createPrivatePropertySymbol(declaration: PropertyDescriptor): IrPropertySymbol {
        return IrPropertySymbolImpl()
    }

    // ------------------------------------ typealias ------------------------------------

    override fun createPublicTypeAliasSymbol(declaration: TypeAliasDescriptor, signature: IdSignature): IrTypeAliasSymbol {
        return IrTypeAliasSymbolImpl(signature)
    }

    override fun createPrivateTypeAliasSymbol(declaration: TypeAliasDescriptor): IrTypeAliasSymbol {
        return IrTypeAliasSymbolImpl()
    }

    // ------------------------------------ function ------------------------------------

    override fun createPublicFunctionSymbol(declaration: FunctionDescriptor, signature: IdSignature): IrSimpleFunctionSymbol {
        return IrSimpleFunctionSymbolImpl(signature)
    }

    override fun createPrivateFunctionSymbol(declaration: FunctionDescriptor): IrSimpleFunctionSymbol {
        return IrSimpleFunctionSymbolImpl()
    }

    // ------------------------------------ type parameter ------------------------------------

    override fun createPublicTypeParameterSymbol(declaration: TypeParameterDescriptor, signature: IdSignature): IrTypeParameterSymbol {
        return IrTypeParameterSymbolImpl(signature)
    }

    override fun createPrivateTypeParameterSymbol(declaration: TypeParameterDescriptor): IrTypeParameterSymbol {
        return IrTypeParameterSymbolImpl()
    }

    // ------------------------------------ value parameter ------------------------------------

    override fun referenceValueParameter(declaration: ParameterDescriptor): IrValueParameterSymbol {
        return valueParameterSlice.referenced(declaration) {
            error("Undefined parameter referenced: $declaration\n${valueParameterSlice.dump()}")
        }
    }
}

