/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns

class DescriptorTable {
    private val descriptors = mutableMapOf<DeclarationDescriptor, Long>()
    fun put(descriptor: DeclarationDescriptor, uniqId: UniqId) {
        descriptors.getOrPut(descriptor) { uniqId.index }
    }
    fun get(descriptor: DeclarationDescriptor) = descriptors[descriptor]
}

// TODO: We don't manage id clashes anyhow now.
abstract class DeclarationTable(val builtIns: IrBuiltIns, val descriptorTable: DescriptorTable, mangler: KotlinMangler): KotlinMangler by mangler {

    private val table = mutableMapOf<IrDeclaration, UniqId>()
    val debugIndex = mutableMapOf<UniqId, String>()
    val descriptors = descriptorTable
    abstract protected var currentIndex: Long
    protected var initialized: Boolean = false

    open fun loadKnownBuiltins() {
        builtIns.knownBuiltins.forEach {
            table.put(it, UniqId(currentIndex++, false))
        }
        initialized = true
    }

    fun uniqIdByDeclaration(value: IrDeclaration) = table.getOrPut(value) {
        if (!initialized) error("DeclarationTable has not been initialized")
        computeUniqIdByDeclaration(value)
    }

    open protected fun computeUniqIdByDeclaration(value: IrDeclaration): UniqId {
        val index = if (value.origin == IrDeclarationOrigin.FAKE_OVERRIDE ||
            !value.isExported()
            || value is IrVariable
            || value is IrTypeParameter
            || value is IrValueParameter
            || value is IrAnonymousInitializerImpl
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
    get() = irBuiltInsExternalPackageFragment.declarations
