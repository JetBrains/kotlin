/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrLock
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.*

@OptIn(ObsoleteDescriptorBasedAPI::class)
class IrLazySymbolTable(private val originalTable: SymbolTable) : ReferenceSymbolTable by originalTable {
    /*Don't force builtins class linking before unbound symbols linking: otherwise stdlib compilation will fail*/
    var stubGenerator: DeclarationStubGenerator? = null

    @ObsoleteDescriptorBasedAPI
    override val descriptorExtension: DescriptorBasedReferenceSymbolTableExtension = ExtensionWrapper()

    private inner class ExtensionWrapper : DescriptorSymbolTableExtension(originalTable) {
        private val delegate get() = originalTable.descriptorExtension

        override fun referenceClass(declaration: ClassDescriptor): IrClassSymbol {
            synchronized(lock) {
                return delegate.referenceClass(declaration).also {
                    if (!it.isBound) {
                        stubGenerator?.generateClassStub(declaration)
                    }
                }
            }
        }

        override fun referenceTypeAlias(declaration: TypeAliasDescriptor): IrTypeAliasSymbol {
            synchronized(lock) {
                return delegate.referenceTypeAlias(declaration).also {
                    if (!it.isBound) {
                        stubGenerator?.generateTypeAliasStub(declaration)
                    }
                }
            }
        }

        override fun referenceConstructor(declaration: ClassConstructorDescriptor): IrConstructorSymbol {
            synchronized(lock) {
                return delegate.referenceConstructor(declaration).also {
                    if (!it.isBound) {
                        stubGenerator?.generateConstructorStub(declaration)
                    }
                }
            }
        }

        override fun referenceEnumEntry(declaration: ClassDescriptor): IrEnumEntrySymbol {
            synchronized(lock) {
                return delegate.referenceEnumEntry(declaration).also {
                    if (!it.isBound) {
                        stubGenerator?.generateEnumEntryStub(declaration)
                    }
                }
            }
        }

        override fun referenceSimpleFunction(declaration: FunctionDescriptor): IrSimpleFunctionSymbol {
            synchronized(lock) {
                return delegate.referenceSimpleFunction(declaration).also {
                    if (!it.isBound) {
                        stubGenerator?.generateFunctionStub(declaration)
                    }
                }
            }
        }

        override fun referenceProperty(declaration: PropertyDescriptor): IrPropertySymbol {
            synchronized(lock) {
                return delegate.referenceProperty(declaration).also {
                    if (!it.isBound) {
                        stubGenerator?.generatePropertyStub(declaration)
                    }
                }
            }
        }

        override fun referenceTypeParameter(declaration: TypeParameterDescriptor): IrTypeParameterSymbol {
            synchronized(lock) {
                return delegate.referenceTypeParameter(declaration).also {
                    if (!it.isBound) {
                        stubGenerator?.generateOrGetTypeParameterStub(declaration)
                    }
                }
            }
        }

        override fun referenceValueParameter(declaration: ParameterDescriptor): IrValueParameterSymbol {
            return delegate.referenceValueParameter(declaration)
        }

        override fun referenceValue(value: ValueDescriptor): IrValueSymbol {
            return delegate.referenceValue(value)
        }

        override fun referenceScript(declaration: ScriptDescriptor): IrScriptSymbol {
            return delegate.referenceScript(declaration)
        }

        override fun referenceField(declaration: PropertyDescriptor): IrFieldSymbol {
            return delegate.referenceField(declaration)
        }

        override fun referenceDeclaredFunction(declaration: FunctionDescriptor): IrSimpleFunctionSymbol {
            return delegate.referenceDeclaredFunction(declaration)
        }

        override fun referenceScopedTypeParameter(declaration: TypeParameterDescriptor): IrTypeParameterSymbol {
            return delegate.referenceScopedTypeParameter(declaration)
        }
    }
}
