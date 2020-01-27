/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler


interface IdSignatureClashTracker {
    fun commit(declaration: IrDeclaration, signature: IdSignature)

    companion object {
        val DEFAULT_TRACKER = object : IdSignatureClashTracker {
            override fun commit(declaration: IrDeclaration, signature: IdSignature) {}
        }
    }
}

abstract class GlobalDeclarationTable(
    val signaturer: IdSignatureSerializer,
    private val mangler: KotlinMangler.IrMangler,
    private val clashTracker: IdSignatureClashTracker
) {
    private val table = mutableMapOf<IrDeclaration, IdSignature>()

    constructor(signaturer: IdSignatureSerializer, mangler: KotlinMangler.IrMangler) :
            this(signaturer, mangler, IdSignatureClashTracker.DEFAULT_TRACKER)

    protected fun loadKnownBuiltins(builtIns: IrBuiltIns) {
        builtIns.knownBuiltins.forEach {
            val symbol = (it as IrSymbolOwner).symbol
            table[it] = symbol.signature.also { id -> clashTracker.commit(it, id) }
        }
    }

    open fun computeSignatureByDeclaration(declaration: IrDeclaration): IdSignature {
        return table.getOrPut(declaration) {
            signaturer.composePublicIdSignature(declaration).also { clashTracker.commit(declaration, it) }
        }
    }

    fun isExportedDeclaration(declaration: IrDeclaration): Boolean = with(mangler) { declaration.isExported() }
}

open class DeclarationTable(private val globalDeclarationTable: GlobalDeclarationTable) {
    private val table = mutableMapOf<IrDeclaration, IdSignature>()
    private val signaturer = globalDeclarationTable.signaturer.also {
        it.reset()
        it.table = this
    }

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

    fun signatureByDeclaration(declaration: IrDeclaration): IdSignature {
        return computeSignatureByDeclaration(declaration)
    }
}

// This is what we pre-populate tables with
val IrBuiltIns.knownBuiltins: List<IrDeclaration>
    get() = packageFragment.declarations
