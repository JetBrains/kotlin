/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.diagnostics.IdSignatureClashDetector
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureFactory
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.render

abstract class GlobalDeclarationTable(private val mangler: KotlinMangler.IrMangler) {
    val publicIdSignatureComputer = PublicIdSignatureComputer(mangler)
    internal val clashDetector = IdSignatureClashDetector()

    protected val table = hashMapOf<IrDeclaration, IdSignature>()

    protected fun loadKnownBuiltins(builtIns: IrBuiltIns) {
        builtIns.knownBuiltins.forEach {
            val symbol = (it as IrSymbolOwner).symbol
            table[it] = symbol.signature!!.also { id -> clashDetector.trackDeclaration(it, id) }
        }
    }

    fun computeSignatureByDeclaration(
        declaration: IrDeclaration,
        compatibleMode: Boolean,
        recordInSignatureClashDetector: Boolean,
    ): IdSignature {
        return table.getOrPut(declaration) {
            publicIdSignatureComputer.composePublicIdSignature(declaration, compatibleMode)
        }.also {
            if (recordInSignatureClashDetector && it.isPubliclyVisible && !it.isLocal) {
                clashDetector.trackDeclaration(declaration, it)
            }
        }
    }

    fun isExportedDeclaration(declaration: IrDeclaration, compatibleMode: Boolean): Boolean = with(mangler) { declaration.isExported(compatibleMode) }
}

open class DeclarationTable(globalTable: GlobalDeclarationTable) {
    protected val table = hashMapOf<IrDeclaration, IdSignature>()
    protected open val globalDeclarationTable: GlobalDeclarationTable = globalTable
    // TODO: we need to disentangle signature construction with declaration tables.
    open val signaturer: IdSignatureFactory = IdSignatureFactory(globalTable.publicIdSignatureComputer, this)

    fun <R> inFile(file: IrFile?, block: () -> R): R =
        signaturer.inFile(file?.symbol, block)

    private fun IrDeclaration.isLocalDeclaration(compatibleMode: Boolean): Boolean {
        return !isExportedDeclaration(this, compatibleMode)
    }

    fun isExportedDeclaration(declaration: IrDeclaration, compatibleMode: Boolean) =
        globalDeclarationTable.isExportedDeclaration(declaration, compatibleMode)

    protected open fun tryComputeBackendSpecificSignature(declaration: IrDeclaration): IdSignature? = null

    private fun allocateIndexedSignature(declaration: IrDeclaration, compatibleMode: Boolean): IdSignature {
        return table.getOrPut(declaration) { signaturer.composeFileLocalIdSignature(declaration, compatibleMode) }
    }

    fun privateDeclarationSignature(declaration: IrDeclaration, compatibleMode: Boolean, builder: () -> IdSignature): IdSignature {
        assert(declaration.isLocalDeclaration(compatibleMode))
        return table.getOrPut(declaration) { builder() }
    }

    fun signatureByDeclaration(declaration: IrDeclaration, compatibleMode: Boolean, recordInSignatureClashDetector: Boolean): IdSignature {
        tryComputeBackendSpecificSignature(declaration)?.let { return it }
        return if (declaration.isLocalDeclaration(compatibleMode)) {
            allocateIndexedSignature(declaration, compatibleMode)
        } else {
            globalDeclarationTable.computeSignatureByDeclaration(declaration, compatibleMode, recordInSignatureClashDetector)
        }
    }

    fun assumeDeclarationSignature(declaration: IrDeclaration, signature: IdSignature) {
        assert(table[declaration] == null) { "Declaration table already has signature for ${declaration.render()}" }
        table.put(declaration, signature)
    }
}

// This is what we pre-populate tables with
val IrBuiltIns.knownBuiltins: List<IrDeclaration>
    get() = operatorsPackageFragment.declarations
