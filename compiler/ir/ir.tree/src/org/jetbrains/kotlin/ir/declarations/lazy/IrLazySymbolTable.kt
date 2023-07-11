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

    val lock: IrLock get() = originalTable.lock

    /*Don't force builtins class linking before unbound symbols linking: otherwise stdlib compilation will failed*/
    var stubGenerator: DeclarationStubGenerator? = null

    @ObsoleteDescriptorBasedAPI
    override val descriptorExtension: DescriptorBasedReferenceSymbolTableExtension = ExtensionWrapper()

    private inner class ExtensionWrapper : DescriptorSymbolTableExtension(originalTable) {
        private val delegate get() = originalTable.descriptorExtension

        override fun referenceClass(descriptor: ClassDescriptor): IrClassSymbol {
            synchronized(lock) {
                return delegate.referenceClass(descriptor).also {
                    if (!it.isBound) {
                        stubGenerator?.generateClassStub(descriptor)
                    }
                }
            }
        }

        override fun referenceTypeAlias(descriptor: TypeAliasDescriptor): IrTypeAliasSymbol {
            synchronized(lock) {
                return delegate.referenceTypeAlias(descriptor).also {
                    if (!it.isBound) {
                        stubGenerator?.generateTypeAliasStub(descriptor)
                    }
                }
            }
        }

        override fun referenceConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol {
            synchronized(lock) {
                return delegate.referenceConstructor(descriptor).also {
                    if (!it.isBound) {
                        stubGenerator?.generateConstructorStub(descriptor)
                    }
                }
            }
        }

        override fun referenceEnumEntry(descriptor: ClassDescriptor): IrEnumEntrySymbol {
            synchronized(lock) {
                return delegate.referenceEnumEntry(descriptor).also {
                    if (!it.isBound) {
                        stubGenerator?.generateEnumEntryStub(descriptor)
                    }
                }
            }
        }

        override fun referenceSimpleFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol {
            synchronized(lock) {
                return delegate.referenceSimpleFunction(descriptor).also {
                    if (!it.isBound) {
                        stubGenerator?.generateFunctionStub(descriptor)
                    }
                }
            }
        }

        override fun referenceProperty(descriptor: PropertyDescriptor): IrPropertySymbol {
            synchronized(lock) {
                return delegate.referenceProperty(descriptor).also {
                    if (!it.isBound) {
                        stubGenerator?.generatePropertyStub(descriptor)
                    }
                }
            }
        }

        override fun referenceTypeParameter(classifier: TypeParameterDescriptor): IrTypeParameterSymbol {
            synchronized(lock) {
                return delegate.referenceTypeParameter(classifier).also {
                    if (!it.isBound) {
                        stubGenerator?.generateOrGetTypeParameterStub(classifier)
                    }
                }
            }
        }

        override fun referenceValueParameter(descriptor: ParameterDescriptor): IrValueParameterSymbol {
            return delegate.referenceValueParameter(descriptor)
        }

        override fun referenceValue(value: ValueDescriptor): IrValueSymbol {
            return delegate.referenceValue(value)
        }

        override fun referenceScript(descriptor: ScriptDescriptor): IrScriptSymbol {
            return delegate.referenceScript(descriptor)
        }

        override fun referenceField(descriptor: PropertyDescriptor): IrFieldSymbol {
            return delegate.referenceField(descriptor)
        }

        override fun referenceDeclaredFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol {
            return delegate.referenceDeclaredFunction(descriptor)
        }

        override fun referenceScopedTypeParameter(classifier: TypeParameterDescriptor): IrTypeParameterSymbol {
            return delegate.referenceScopedTypeParameter(classifier)
        }
    }
}
