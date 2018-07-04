/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable


class IrLazySymbolTable(private val stubGenerator: DeclarationStubGenerator, val originalTable: SymbolTable) : SymbolTable() {

    /*Don't force builtins class linking before unbound symbols linking: otherwise stdlib compilation will failed*/
    internal var unboundSymbolGeneration = false

    override fun referenceClass(descriptor: ClassDescriptor): IrClassSymbol {
        return originalTable.referenceClass(descriptor).also {
            if (unboundSymbolGeneration && !it.isBound) {
                stubGenerator.generateClassStub(descriptor).symbol
            }
        }
    }

    override fun referenceConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol {
        return originalTable.referenceConstructor(descriptor).also {
            if (!it.isBound && unboundSymbolGeneration) {
                stubGenerator.generateConstructorStub(descriptor).symbol
            }
        }
    }

    override fun referenceEnumEntry(descriptor: ClassDescriptor): IrEnumEntrySymbol {
        return originalTable.referenceEnumEntry(descriptor).also {
            if (!it.isBound && unboundSymbolGeneration) {
                stubGenerator.generateEnumEntryStub(descriptor).symbol
            }
        }
    }

    override fun referenceSimpleFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol {
        return originalTable.referenceSimpleFunction(descriptor).also {
            if (!it.isBound && unboundSymbolGeneration) {
                stubGenerator.generateFunctionStub(descriptor).symbol
            }
        }
    }

    override fun referenceTypeParameter(classifier: TypeParameterDescriptor): IrTypeParameterSymbol {
        return originalTable.referenceTypeParameter(classifier).also {
            if (!it.isBound && unboundSymbolGeneration) {
                stubGenerator.generateOrGetTypeParameterStub(classifier).symbol
            }
        }
    }
}