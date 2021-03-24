/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable

interface SymbolTableInterceptor {
    fun referenceValue(symbolTable: SymbolTable, descriptor: VariableDescriptor): IrValueSymbol
    fun referenceValueParameter(symbolTable: SymbolTable, descriptor: ReceiverParameterDescriptor): IrValueSymbol
    fun remapDescriptor(symbolTable: SymbolTable, descriptor: DeclarationDescriptor): IrValueParameterSymbol?
}

class PassThroughSymbolTableInterceptor : SymbolTableInterceptor {
    override fun referenceValue(symbolTable: SymbolTable, descriptor: VariableDescriptor): IrValueSymbol {
        return symbolTable.referenceValue(descriptor)
    }

    override fun referenceValueParameter(symbolTable: SymbolTable, descriptor: ReceiverParameterDescriptor): IrValueSymbol {
        return symbolTable.referenceValueParameter(descriptor)
    }

    override fun remapDescriptor(symbolTable: SymbolTable, descriptor: DeclarationDescriptor): IrValueParameterSymbol? = null
}