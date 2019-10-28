/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns

class DescriptorTable {
    private val descriptors = mutableMapOf<DeclarationDescriptor, Long>()
    fun put(descriptor: DeclarationDescriptor, uniqId: UniqId) {
        descriptors.getOrPut(descriptor) { uniqId.index }
    }

    fun get(descriptor: DeclarationDescriptor) = descriptors[descriptor]
}

interface UniqIdClashTracker {
    fun commit(declaration: IrDeclaration, uniqId: UniqId)

    companion object {
        val DEFAULT_TRACKER = object : UniqIdClashTracker {
            override fun commit(declaration: IrDeclaration, uniqId: UniqId) {}
        }
    }
}

abstract class GlobalDeclarationTable(private val mangler: KotlinMangler, private val clashTracker: UniqIdClashTracker) {
    private val table = mutableMapOf<IrDeclaration, UniqId>()

    constructor(mangler: KotlinMangler) : this(mangler, UniqIdClashTracker.DEFAULT_TRACKER)

    protected fun loadKnownBuiltins(builtIns: IrBuiltIns) {
        val mask = 1L shl 63
        builtIns.knownBuiltins.forEach {
            val index = with(mangler) { it.mangle.hashMangle }
            table[it] = UniqId(index or mask).also { id -> clashTracker.commit(it, id) }
        }
    }

    open fun computeUniqIdByDeclaration(declaration: IrDeclaration): UniqId {
        return table.getOrPut(declaration) {
            with(mangler) {
                UniqId(declaration.hashedMangle).also { clashTracker.commit(declaration, it) }
            }
        }
    }

    fun isExportedDeclaration(declaration: IrDeclaration): Boolean = with(mangler) { declaration.isExported() }
}

open class DeclarationTable(
    private val descriptorTable: DescriptorTable,
    private val globalDeclarationTable: GlobalDeclarationTable,
    startIndex: Long
) {
    private val table = mutableMapOf<IrDeclaration, UniqId>()

    private fun IrDeclaration.isLocalDeclaration(): Boolean {
        return origin == IrDeclarationOrigin.FAKE_OVERRIDE || !isExportedDeclaration(this) || this is IrValueDeclaration || this is IrAnonymousInitializer || this is IrLocalDelegatedProperty
    }

    private var localIndex = startIndex

    fun isExportedDeclaration(declaration: IrDeclaration) = globalDeclarationTable.isExportedDeclaration(declaration)

    protected open fun tryComputeBackendSpecificUniqId(declaration: IrDeclaration): UniqId? =
            null

    private fun computeUniqIdByDeclaration(declaration: IrDeclaration): UniqId {
        tryComputeBackendSpecificUniqId(declaration)?.let { return it }
        return if (declaration.isLocalDeclaration()) {
            table.getOrPut(declaration) { UniqId(localIndex++) }
        } else globalDeclarationTable.computeUniqIdByDeclaration(declaration)
    }

    fun uniqIdByDeclaration(declaration: IrDeclaration): UniqId {
        val uniqId = computeUniqIdByDeclaration(declaration)
        if (declaration.isMetadataDeclaration(false)) {
            descriptorTable.put(declaration.descriptor, uniqId)
        }
        return uniqId
    }

    private tailrec fun IrDeclaration.isMetadataDeclaration(isTypeParameter: Boolean): Boolean {
        if (this is IrValueDeclaration || this is IrField) {
            return false
        }

        if (origin == IrDeclarationOrigin.FAKE_OVERRIDE || origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER) {
            return false
        }

        if (!isTypeParameter) {
            if (this is IrSimpleFunction) {
                if (correspondingPropertySymbol != null)
                    return false
            }
        }

        if (this is IrDeclarationWithVisibility) {
            if (visibility == Visibilities.LOCAL) {
                return false
            }
        }

        if (this is IrLazyDeclarationBase) return false

        if (parent is IrPackageFragment) return true

        return (parent as IrDeclaration).isMetadataDeclaration(this is IrTypeParameter || isTypeParameter)
    }
}

// This is what we pre-populate tables with
val IrBuiltIns.knownBuiltins
    get() = irBuiltInsSymbols
