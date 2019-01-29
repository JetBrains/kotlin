/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.descriptors.getFunction
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.modules.KOTLIN_STDLIB_MODULE_NAME
import java.util.regex.Pattern

fun <K, V> MutableMap<K, V>.putOnce(k:K, v: V): Unit {
    assert(!this.containsKey(k) || this[k] == v) {
        "adding $v for $k, but it is already ${this[k]} for $k"
    }
    this.put(k, v)
}

class DescriptorTable {
    private val descriptors = mutableMapOf<DeclarationDescriptor, Long>()
    fun put(descriptor: DeclarationDescriptor, uniqId: UniqId) {
        descriptors.putOnce(descriptor, uniqId.index)
    }
    fun get(descriptor: DeclarationDescriptor) = descriptors[descriptor]
}

// TODO: We don't manage id clashes anyhow now.
class DeclarationTable(val builtIns: IrBuiltIns, val descriptorTable: DescriptorTable, val symbolTable: SymbolTable) {

    private val table = mutableMapOf<IrDeclaration, UniqId>()
    val debugIndex = mutableMapOf<UniqId, String>()
    val descriptors = descriptorTable
    private var currentIndex = 0x1_0000_0000L

    private val FUNCTION_INDEX_START: Long

    init {
        val known = builtIns.knownBuiltins
        known.forEach {
            table.put(it, UniqId(currentIndex++, false))
        }

        FUNCTION_INDEX_START = currentIndex
        currentIndex += BUILT_IN_UNIQ_ID_GAP
    }

    fun uniqIdByDeclaration(value: IrDeclaration): UniqId {
        val index = table.getOrPut(value) {

            if (isBuiltInFunction(value)) {
                UniqId(FUNCTION_INDEX_START + builtInFunctionId(value), false)
            } else if (value.origin == IrDeclarationOrigin.FAKE_OVERRIDE ||
                !value.isExported()
                    || value is IrVariable
                    || value is IrTypeParameter
                    || value is IrValueParameter
                    || value is IrAnonymousInitializerImpl
            ) {

                val desc = value.descriptor
                if (desc is CallableDescriptor) {
                    if (desc.visibility == Visibilities.PUBLIC || value.origin != IrDeclarationOrigin.FAKE_OVERRIDE) {
                        fun foo(){}
                        foo()
                    }
                }
                UniqId(currentIndex++, true)
            } else {
                UniqId(value.uniqIdIndex, false)
            }
        }

        debugIndex.put(index, "${if (index.isLocal) "" else value.uniqSymbolName()} descriptor = ${value.descriptor}")

        return index
    }
}

// This is what we pre-populate tables with
val IrBuiltIns.knownBuiltins
    get() = irBuiltInsExternalPackageFragment.declarations