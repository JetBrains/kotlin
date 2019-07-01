/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrSymbol

class DescriptorTable {
    private val descriptors = mutableMapOf<DeclarationDescriptor, Long>()
    fun put(descriptor: DeclarationDescriptor, uniqId: UniqId) {
        descriptors.getOrPut(descriptor) { uniqId.index }
    }
    fun get(descriptor: DeclarationDescriptor) = descriptors[descriptor]
}

// TODO: We don't manage id clashes anyhow now.
abstract class DeclarationTable(val builtIns: IrBuiltIns, val descriptorTable: DescriptorTable, mangler: KotlinMangler): KotlinMangler by mangler {

    private val builtInsTable = mutableMapOf<IrSymbol, UniqId>()
    private val table = mutableMapOf<IrDeclaration, UniqId>()
    val descriptors = descriptorTable
    protected abstract var currentIndex: Long

    open fun loadKnownBuiltins(): Long {
        builtIns.knownBuiltins.forEach {
            builtInsTable[it] = UniqId(currentIndex++, false)
        }
        return currentIndex
    }

    fun uniqIdByDeclaration(value: IrDeclaration) = (value as? IrSymbolOwner)?.let { builtInsTable[it.symbol] } ?: table.getOrPut(value) {
        computeUniqIdByDeclaration(value)
    }

    open protected fun computeUniqIdByDeclaration(value: IrDeclaration): UniqId {
        val index = if (value.origin == IrDeclarationOrigin.FAKE_OVERRIDE ||
            !value.isExported()
            || value is IrVariable
            || value is IrValueParameter
            || value is IrAnonymousInitializer
            || value is IrLocalDelegatedProperty
        ) {
            UniqId(currentIndex++, true)
        } else {
            UniqId(value.hashedMangle, false)
        }

        return index
    }
}

// This is what we pre-populate tables with
val IrBuiltIns.knownBuiltins
    get() = irBuiltInsSymbols
