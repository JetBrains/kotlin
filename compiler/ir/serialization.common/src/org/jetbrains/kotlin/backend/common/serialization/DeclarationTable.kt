/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.jetbrains.kotlin.backend.common.diagnostics.IdSignatureClashDetector
import org.jetbrains.kotlin.backend.common.serialization.signature.FileLocalIdSignatureComputer
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler.IrMangler

abstract class GlobalDeclarationTable(val mangler: IrMangler) {
    val publicIdSignatureComputer = PublicIdSignatureComputer(mangler)
    internal val clashDetector = IdSignatureClashDetector()

    protected val table: MutableMap<IrDeclaration, IdSignature> = Object2ObjectOpenHashMap()

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
            publicIdSignatureComputer.computePublicIdSignature(declaration, compatibleMode)
        }.also {
            if (recordInSignatureClashDetector && it.isPubliclyVisible && !it.isLocal) {
                clashDetector.trackDeclaration(declaration, it)
            }
        }
    }
}

abstract class DeclarationTable<GDT : GlobalDeclarationTable>(val globalDeclarationTable: GDT) {
    class Default(globalTable: GlobalDeclarationTable) : DeclarationTable<GlobalDeclarationTable>(globalTable)

    val mangler = globalDeclarationTable.mangler
    protected val table: MutableMap<IrSymbolOwner, IdSignature> = Object2ObjectOpenHashMap()

    private val fileLocalIdSignatureComputer = FileLocalIdSignatureComputer(mangler) { declaration, compatibleMode ->
        signatureByDeclaration(declaration, compatibleMode, recordInSignatureClashDetector = false)
    }

    fun <R> inFile(file: IrFile?, block: () -> R): R =
        globalDeclarationTable.publicIdSignatureComputer.inFile(file?.symbol, block)

    private fun IrDeclaration.shouldHaveLocalSignature(compatibleMode: Boolean): Boolean {
        return with(mangler) { !this@shouldHaveLocalSignature.isExported(compatibleMode) }
    }

    protected open fun tryComputeBackendSpecificSignature(declaration: IrDeclaration): IdSignature? = null

    fun signatureByDeclaration(declaration: IrDeclaration, compatibleMode: Boolean, recordInSignatureClashDetector: Boolean): IdSignature {
        tryComputeBackendSpecificSignature(declaration)?.let { return it }
        return if (declaration.shouldHaveLocalSignature(compatibleMode)) {
            table.getOrPut(declaration) { fileLocalIdSignatureComputer.computeFileLocalIdSignature(declaration, compatibleMode) }
        } else {
            globalDeclarationTable.computeSignatureByDeclaration(declaration, compatibleMode, recordInSignatureClashDetector)
        }
    }

    fun signatureByReturnableBlock(returnableBlock: IrReturnableBlock): IdSignature =
        table.getOrPut(returnableBlock) { fileLocalIdSignatureComputer.generateScopeLocalSignature("RB") }
}

// This is what we pre-populate tables with
val IrBuiltIns.knownBuiltins: List<IrDeclaration>
    get() = operatorsPackageFragment.declarations
