/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.SymbolTable


class IrLazySymbolTable(private val originalTable: SymbolTable) : ReferenceSymbolTable by originalTable {
    override val declarationSystemTable: SymbolTable = originalTable

    override val referenceSymbolTable: ReferenceSymbolTable = this

    /*Don't force builtins class linking before unbound symbols linking: otherwise stdlib compilation will failed*/
    var stubGenerator: DeclarationStubGenerator? = null

    override fun referenceClass(descriptor: ClassDescriptor): IrClassSymbol {
        return originalTable.referenceClass(descriptor).also {
            if (!it.isBound) {
                stubGenerator?.generateClassStub(descriptor)
            }
        }
    }

    override fun referenceConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol {
        return originalTable.referenceConstructor(descriptor).also {
            if (!it.isBound) {
                stubGenerator?.generateConstructorStub(descriptor)
            }
        }
    }

    override fun referenceEnumEntry(descriptor: ClassDescriptor): IrEnumEntrySymbol {
        return originalTable.referenceEnumEntry(descriptor).also {
            if (!it.isBound) {
                stubGenerator?.generateEnumEntryStub(descriptor)
            }
        }
    }

    override fun referenceSimpleFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol {
        return originalTable.referenceSimpleFunction(descriptor).also {
            if (!it.isBound) {
                stubGenerator?.generateFunctionStub(descriptor)
            }
        }
    }

    override fun referenceTypeParameter(classifier: TypeParameterDescriptor): IrTypeParameterSymbol {
        return originalTable.referenceTypeParameter(classifier).also {
            if (!it.isBound) {
                stubGenerator?.generateOrGetTypeParameterStub(classifier)
            }
        }
    }

    override fun referenceFunction(callable: CallableDescriptor): IrFunctionSymbol {
        return super.referenceFunction(callable)
    }

    override fun referenceClassifier(classifier: ClassifierDescriptor): IrClassifierSymbol {
        return super.referenceClassifier(classifier)
    }
}