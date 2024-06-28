/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.IrFileDeserializer
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.resolveFakeOverride

internal class IdSignatureSource(
    val lib: KotlinLibraryFile,
    private val fileSignatureProvider: FileSignatureProvider,
    val symbol: IrSymbol
) {
    val irFile: IrFile
        get() = fileSignatureProvider.irFile

    val srcFile: KotlinSourceFile
        get() = fileSignatureProvider.srcFile
}

internal fun addParentSignatures(
    signatures: Collection<IdSignature>,
    idSignatureToFile: Map<IdSignature, IdSignatureSource>,
    importerLibFile: KotlinLibraryFile,
    importerSrcFile: KotlinSourceFile
): Set<IdSignature> {
    val allSignatures = HashSet<IdSignature>(signatures.size)

    fun addAllParents(sig: IdSignature) {
        val signatureSrc = idSignatureToFile[sig] ?: return
        if (signatureSrc.lib == importerLibFile && signatureSrc.srcFile == importerSrcFile) {
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

private fun collectImplementedSymbol(deserializedSymbols: Map<IdSignature, IrSymbol>): Map<IdSignature, IrSymbol> {
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

/**
 * This sealed class is used to retrieve declaration signatures for a specific source file.
 */
internal sealed class FileSignatureProvider(val irFile: IrFile, val srcFile: KotlinSourceFile) {
    abstract fun getSignatureToIndexMapping(): Map<IdSignature, Int>
    abstract fun getReachableSignatures(): Set<IdSignature>
    abstract fun getImplementedSymbols(): Map<IdSignature, IrSymbol>

    /**
     * This is a basic implementation that allows retrieving signatures from a deserialized klib via [IrFileDeserializer].
     */
    class DeserializedFromKlib(private val fileDeserializer: IrFileDeserializer, srcFile: KotlinSourceFile) : FileSignatureProvider(fileDeserializer.file, srcFile) {
        override fun getSignatureToIndexMapping(): Map<IdSignature, Int> {
            return fileDeserializer.symbolDeserializer.signatureDeserializer.signatureToIndexMapping()
        }

        override fun getReachableSignatures(): Set<IdSignature> {
            return getSignatureToIndexMapping().keys
        }

        override fun getImplementedSymbols(): Map<IdSignature, IrSymbol> {
            // Sometimes linker may leave unbound symbols in IrSymbolDeserializer::deserializedSymbols map.
            // Generally, all unbound symbols must be caught in KotlinIrLinker::checkNoUnboundSymbols,
            // unfortunately it does not work properly in the current implementation.
            // Also, reachable unbound symbols are caught by IrValidator, it works fine, but it works after this place.
            // Filter unbound symbols here, because an error from IC infrastructure about the unbound symbols looks pretty wired
            // and if the unbound symbol is really reachable from IR the error will be fired from IrValidator later.
            // Otherwise, the unbound symbol is unreachable, and it cannot appear in IC dependency graph, so we can ignore them.
            val deserializedSymbols = fileDeserializer.symbolDeserializer.deserializedSymbols.filter { it.value.isBound }
            return collectImplementedSymbol(deserializedSymbols)
        }
    }

    /**
     * This is a special implementation that allows retrieving signatures for function type interfaces.
     * We need this special implementation because function type interfaces are not serialized into klib,
     * and the parent [IrFile] is generated on the fly during klib deserialization.
     * Therefore, there is no corresponding [IrFileDeserializer] for the parent [IrFile].
     * For more details, see [org.jetbrains.kotlin.ir.backend.js.FunctionTypeInterfacePackages.makePackageAccessor].
     */
    class GeneratedFunctionTypeInterface(file: IrFile, srcFile: KotlinSourceFile) : FileSignatureProvider(file, srcFile) {
        private val allSignatures = run {
            val topLevelSymbols = buildMap {
                for (declaration in irFile.declarations) {
                    val signature = declaration.symbol.signature ?: continue
                    put(signature, declaration.symbol)
                }
            }
            collectImplementedSymbol(topLevelSymbols)
        }

        override fun getSignatureToIndexMapping(): Map<IdSignature, Int> = emptyMap()
        override fun getReachableSignatures(): Set<IdSignature> = allSignatures.keys
        override fun getImplementedSymbols(): Map<IdSignature, IrSymbol> = allSignatures
    }
}
