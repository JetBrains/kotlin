/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.SymbolTable

class DescriptorTable {
    private val descriptors = mutableMapOf<DeclarationDescriptor, Long>()
    fun put(descriptor: DeclarationDescriptor, uniqId: UniqId) {
        descriptors.getOrPut(descriptor) { uniqId.index }
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

    fun uniqIdByDeclaration(value: IrDeclaration) = table.getOrPut(value) {
        val index = if (isBuiltInFunction(value)) {
            UniqId(FUNCTION_INDEX_START + builtInFunctionId(value), false)
        } else if (value.origin == IrDeclarationOrigin.FAKE_OVERRIDE ||
            !value.isExported()
            || value is IrVariable
            || value is IrTypeParameter
            || value is IrValueParameter
            || value is IrAnonymousInitializerImpl
        ) {
            UniqId(currentIndex++, true)
        } else {
            UniqId(value.uniqIdIndex, false)
        }

        // It can grow as large as 1/3 of ir/* size.
        // debugIndex.put(index) {
        //     "${if (index.isLocal) "" else value.uniqSymbolName()} descriptor = ${value.descriptor}"
        //}.also {it == null}

        index
    }
}

// This is what we pre-populate tables with
val IrBuiltIns.knownBuiltins
    get() = irBuiltInsExternalPackageFragment.declarations