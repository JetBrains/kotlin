/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable

interface SymbolTableInterceptor {
    fun referenceValue(symbolTable: SymbolTable, descriptor: VariableDescriptor): IrValueSymbol
}

class PassThroughSymbolTableInterceptor : SymbolTableInterceptor {
    override fun referenceValue(symbolTable: SymbolTable, descriptor: VariableDescriptor): IrValueSymbol {
        return symbolTable.referenceValue(descriptor)
    }
}