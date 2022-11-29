/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.resolveFakeOverride

internal data class IdSignatureSource(val lib: KotlinLibraryFile, val src: KotlinSourceFile, val symbol: IrSymbol)

internal fun addParentSignatures(
    signatures: Collection<IdSignature>,
    idSignatureToFile: Map<IdSignature, IdSignatureSource>,
    importerLibFile: KotlinLibraryFile,
    importerSrcFile: KotlinSourceFile
): Set<IdSignature> {
    val allSignatures = HashSet<IdSignature>(signatures.size)

    fun addAllParents(sig: IdSignature) {
        val signatureSrc = idSignatureToFile[sig] ?: return
        if (signatureSrc.lib == importerLibFile && signatureSrc.src == importerSrcFile) {
            return
        }
        if (allSignatures.add(sig)) {
            (signatureSrc.symbol.owner as? IrDeclaration)?.let { declaration ->
                (declaration.parent as? IrSymbolOwner)?.let { parent ->
                    parent.symbol.signature?.let(::addAllParents)
                }
            }
        }
    }

    signatures.forEach(::addAllParents)

    return allSignatures
}

internal fun resolveFakeOverrideFunction(symbol: IrSymbol): IdSignature? {
    return (symbol.owner as? IrSimpleFunction)?.let { overridable ->
        if (overridable.isFakeOverride) {
            overridable.resolveFakeOverride()?.symbol?.signature
        } else {
            null
        }
    }
}

internal fun collectImplementedSymbol(deserializedSymbols: Map<IdSignature, IrSymbol>): Map<IdSignature, IrSymbol> {
    return HashMap<IdSignature, IrSymbol>(deserializedSymbols.size).apply {
        for ((signature, symbol) in deserializedSymbols) {
            put(signature, symbol)

            fun <T> addSymbol(decl: T): Boolean where T : IrDeclarationWithVisibility, T : IrSymbolOwner {
                when (decl.visibility) {
                    DescriptorVisibilities.LOCAL -> return false
                    DescriptorVisibilities.PRIVATE -> return false
                    DescriptorVisibilities.PRIVATE_TO_THIS -> return false
                }

                val sig = decl.symbol.signature
                if (sig != null && sig !in deserializedSymbols) {
                    return put(sig, decl.symbol) == null
                }
                return false
            }

            fun addNestedDeclarations(irClass: IrClass) {
                for (decl in irClass.declarations) {
                    when (decl) {
                        is IrSimpleFunction -> addSymbol(decl)
                        is IrProperty -> {
                            decl.getter?.let(::addSymbol)
                            decl.setter?.let(::addSymbol)
                        }

                        is IrClass -> {
                            if (addSymbol(decl)) {
                                addNestedDeclarations(decl)
                            }
                        }
                    }
                }
            }

            (symbol.owner as? IrClass)?.let(::addNestedDeclarations)
        }
    }
}
