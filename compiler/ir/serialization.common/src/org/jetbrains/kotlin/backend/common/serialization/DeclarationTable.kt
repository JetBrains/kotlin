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
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
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
            table[it] = symbol.signature!!.also { id -> clashDetector.trackDeclarationIfInCurrentModule(it, id) }
        }
    }

    open fun computeSignatureByDeclaration(declaration: IrDeclaration, compatibleMode: Boolean): IdSignature {
        return table.getOrPut(declaration) {
            publicIdSignatureComputer.composePublicIdSignature(declaration, compatibleMode).also {
                clashDetector.trackDeclarationIfInCurrentModule(declaration, it)
            }
        }
    }

    fun isExportedDeclaration(declaration: IrDeclaration, compatibleMode: Boolean): Boolean = with(mangler) { declaration.isExported(compatibleMode) }
}

private fun IdSignatureClashDetector.trackDeclarationIfInCurrentModule(declaration: IrDeclaration, signature: IdSignature) {
    // Only count signature clashes on the declarations declared in the module being serialized.
    // If there is a declaration in an external module with the same signature as some declaration in the current module,
    // so be it (for now).
    // Note that when we are serializing a module into a KLIB, the declarations from other modules always come in the form of Lazy IR,
    // so this check is enough to ensure that this declaration is declared in the module currently being serialized.
    if (declaration !is IrLazyDeclarationBase) {
        trackDeclaration(declaration, signature)
    }
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

    private fun computeSignatureByDeclaration(declaration: IrDeclaration, compatibleMode: Boolean): IdSignature {
        tryComputeBackendSpecificSignature(declaration)?.let { return it }
        return if (declaration.isLocalDeclaration(compatibleMode)) {
            allocateIndexedSignature(declaration, compatibleMode)
        } else globalDeclarationTable.computeSignatureByDeclaration(declaration, compatibleMode)
    }

    fun privateDeclarationSignature(declaration: IrDeclaration, compatibleMode: Boolean, builder: () -> IdSignature): IdSignature {
        assert(declaration.isLocalDeclaration(compatibleMode))
        return table.getOrPut(declaration) { builder() }
    }

    open fun signatureByDeclaration(declaration: IrDeclaration, compatibleMode: Boolean): IdSignature {
        return computeSignatureByDeclaration(declaration, compatibleMode)
    }

    fun assumeDeclarationSignature(declaration: IrDeclaration, signature: IdSignature) {
        assert(table[declaration] == null) { "Declaration table already has signature for ${declaration.render()}" }
        table.put(declaration, signature)
    }
}

// This is what we pre-populate tables with
val IrBuiltIns.knownBuiltins: List<IrDeclaration>
    get() = operatorsPackageFragment.declarations
