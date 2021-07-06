/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.render


interface IdSignatureClashTracker {
    fun commit(declaration: IrDeclaration, signature: IdSignature)

    companion object {
        val DEFAULT_TRACKER = object : IdSignatureClashTracker {
            override fun commit(declaration: IrDeclaration, signature: IdSignature) {}
        }
    }
}

abstract class GlobalDeclarationTable(
    private val mangler: KotlinMangler.IrMangler,
    private val clashTracker: IdSignatureClashTracker
) {
    val publicIdSignatureComputer = PublicIdSignatureComputer(mangler)

    protected val table = mutableMapOf<IrDeclaration, IdSignature>()

    constructor(mangler: KotlinMangler.IrMangler) : this(mangler, IdSignatureClashTracker.DEFAULT_TRACKER)

    protected fun loadKnownBuiltins(builtIns: IrBuiltIns) {
        builtIns.knownBuiltins.forEach {
            val symbol = (it as IrSymbolOwner).symbol
            table[it] = symbol.signature!!.also { id -> clashTracker.commit(it, id) }
        }
    }

    open fun computeSignatureByDeclaration(declaration: IrDeclaration): IdSignature {
        return table.getOrPut(declaration) {
            publicIdSignatureComputer.composePublicIdSignature(declaration).also { clashTracker.commit(declaration, it) }
        }
    }

    fun isExportedDeclaration(declaration: IrDeclaration): Boolean = with(mangler) { declaration.isExported() }
}

open class DeclarationTable(globalTable: GlobalDeclarationTable) {
    protected val table = mutableMapOf<IrDeclaration, IdSignature>()
    protected open val globalDeclarationTable: GlobalDeclarationTable = globalTable
    // TODO: we need to disentangle signature construction with declaration tables.
    open val signaturer: IdSignatureSerializer = IdSignatureSerializer(globalTable.publicIdSignatureComputer, this)

    private fun IrDeclaration.isLocalDeclaration(): Boolean {
        return !isExportedDeclaration(this)
    }

    fun isExportedDeclaration(declaration: IrDeclaration) = globalDeclarationTable.isExportedDeclaration(declaration)

    protected open fun tryComputeBackendSpecificSignature(declaration: IrDeclaration): IdSignature? = null

    private fun computeSignatureByDeclaration(declaration: IrDeclaration): IdSignature {
        tryComputeBackendSpecificSignature(declaration)?.let { return it }
        return if (declaration.isLocalDeclaration()) {
            table.getOrPut(declaration) { signaturer.composeFileLocalIdSignature(declaration) }
        } else globalDeclarationTable.computeSignatureByDeclaration(declaration)
    }

    fun privateDeclarationSignature(declaration: IrDeclaration, builder: () -> IdSignature): IdSignature {
        assert(declaration.isLocalDeclaration())
        return table.getOrPut(declaration) { builder() }
    }

    open fun signatureByDeclaration(declaration: IrDeclaration): IdSignature {
        return computeSignatureByDeclaration(declaration)
    }

    fun assumeDeclarationSignature(declaration: IrDeclaration, signature: IdSignature) {
        assert(table[declaration] == null) { "Declaration table already has signature for ${declaration.render()}" }
        table.put(declaration, signature)
    }
}

// This is what we pre-populate tables with
val IrBuiltIns.knownBuiltins: List<IrDeclaration>
    get() = packageFragment.declarations
