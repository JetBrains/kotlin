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
import org.jetbrains.kotlin.ir.util.KotlinMangler.IrMangler
import org.jetbrains.kotlin.ir.util.render

abstract class GlobalDeclarationTable(val mangler: IrMangler) {
    val publicIdSignatureComputer = PublicIdSignatureComputer(mangler)
    internal val clashDetector = IdSignatureClashDetector()

    protected val table = hashMapOf<IrDeclaration, IdSignature>()

    protected fun loadKnownBuiltins(builtIns: IrBuiltIns) {
        builtIns.knownBuiltins.forEach {
            val symbol = (it as IrSymbolOwner).symbol
            when (val signature = symbol.signature) {
                null -> computeSignatureByDeclaration(it, compatibleMode = false, recordInSignatureClashDetector = true)
                else -> {
                    table[it] = signature
                    clashDetector.trackDeclaration(it, signature)
                }
            }
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
}

abstract class DeclarationTable<GDT : GlobalDeclarationTable>(val globalDeclarationTable: GDT) {
    class Default(globalTable: GlobalDeclarationTable) : DeclarationTable<GlobalDeclarationTable>(globalTable)

    protected val table = hashMapOf<IrDeclaration, IdSignature>()

    // TODO: we need to disentangle signature construction with declaration tables.
    val signaturer: IdSignatureFactory = IdSignatureFactory(globalDeclarationTable.publicIdSignatureComputer, this)

    fun <R> inFile(file: IrFile?, block: () -> R): R =
        signaturer.inFile(file?.symbol, block)

    private fun IrDeclaration.shouldHaveLocalSignature(compatibleMode: Boolean): Boolean {
        return with(globalDeclarationTable.mangler) { !this@shouldHaveLocalSignature.isExported(compatibleMode) }
    }

    protected open fun tryComputeBackendSpecificSignature(declaration: IrDeclaration): IdSignature? = null

    private fun allocateIndexedSignature(declaration: IrDeclaration, compatibleMode: Boolean): IdSignature {
        return table.getOrPut(declaration) { signaturer.composeFileLocalIdSignature(declaration, compatibleMode) }
    }

    fun privateDeclarationSignature(declaration: IrDeclaration, compatibleMode: Boolean, builder: () -> IdSignature): IdSignature {
        assert(declaration.shouldHaveLocalSignature(compatibleMode))
        return table.getOrPut(declaration) { builder() }
    }

    fun signatureByDeclaration(declaration: IrDeclaration, compatibleMode: Boolean, recordInSignatureClashDetector: Boolean): IdSignature {
        tryComputeBackendSpecificSignature(declaration)?.let { return it }
        return if (declaration.shouldHaveLocalSignature(compatibleMode)) {
            allocateIndexedSignature(declaration, compatibleMode)
        } else {
            globalDeclarationTable.computeSignatureByDeclaration(declaration, compatibleMode, recordInSignatureClashDetector)
        }
    }

    fun assumeDeclarationSignature(declaration: IrDeclaration, signature: IdSignature) {
        assert(table[declaration] == null) { "Declaration table already has signature for ${declaration.render()}" }
        table[declaration] = signature
    }
}

// This is what we pre-populate tables with
val IrBuiltIns.knownBuiltins: List<IrDeclaration>
    get() = operatorsPackageFragment.declarations
