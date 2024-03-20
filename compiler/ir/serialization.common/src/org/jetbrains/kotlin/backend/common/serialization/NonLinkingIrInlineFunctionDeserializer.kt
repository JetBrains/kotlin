/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class NonLinkingIrInlineFunctionDeserializer(
    private val irBuiltIns: IrBuiltIns,
    libraries: List<KotlinLibrary>
) {
    private val irInterner = IrInterningService()
    private val irFactory get() = irBuiltIns.irFactory

    /**
     * This is a separate symbol table ("detached") from the symbol table ("main") that is used in IR linker.
     *
     * The goal is to separate the linkage process, which should end with all symbols been bound to the respective declarations,
     * and the process of partial deserialization of inline functions, which should produce some amount of unbound symbols
     * that are not supposed to be linked and therefore should not be tracked in the main symbol table.
     */
    private val detachedSymbolTable = SymbolTable(signaturer = null, irFactory)

    private val moduleDeserializers = libraries.map(::ModuleDeserializer)

    // TODO: consider the case of `external inline` functions that exist in Kotlin/Native stdlib
    fun deserializeInlineFunction(signature: IdSignature): IrSimpleFunction {
        val topLevelSignature = signature.topLevelSignature()
        val topLevelDeclaration = moduleDeserializers.firstNotNullOfOrNull { it.getTopLevelDeclarationOrNull(topLevelSignature) }

        val function = when {
            topLevelDeclaration == null -> null

            signature == topLevelSignature -> topLevelDeclaration as? IrSimpleFunction

            else -> {
                val symbol = referencePublicSymbol(signature, BinarySymbolData.SymbolKind.FUNCTION_SYMBOL)
                runIf(symbol.isBound) { symbol.owner as IrSimpleFunction }
            }
        }

        check(function != null) { "Function not found: $signature" }
        check(function.isInline) { "Non-inline function: $signature" }

        return function
    }

    private fun referencePublicSymbol(signature: IdSignature, symbolKind: BinarySymbolData.SymbolKind) =
        referenceDeserializedSymbol(detachedSymbolTable, fileSymbol = null, symbolKind, signature)

    private inner class ModuleDeserializer(library: KotlinLibrary) {
        init {
            check(library.hasIr) { "Ir-less library: ${library.libraryFile.path}" }
        }

        private val fileDeserializers = (0 until library.fileCount()).map { fileIndex ->
            FileDeserializer(library, fileIndex)
        }

        fun getTopLevelDeclarationOrNull(topLevelSignature: IdSignature): IrDeclaration? =
            fileDeserializers.firstNotNullOfOrNull { it.getTopLevelDeclarationOrNull(topLevelSignature) }
    }

    private inner class FileDeserializer(library: KotlinLibrary, fileIndex: Int) {
        private val fileProto = ProtoFile.parseFrom(library.file(fileIndex).codedInputStream, ExtensionRegistryLite.newInstance())
        private val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(library, fileIndex))

        private val dummyFileSymbol = IrFileSymbolImpl().apply {
            IrFileImpl(
                fileEntry = NaiveSourceBasedFileEntryImpl(fileProto.fileEntry.name),
                symbol = this,
                packageFqName = FqName(irInterner.string(fileReader.deserializeFqName(fileProto.fqNameList)))
            )
        }

        private val symbolDeserializer = IrSymbolDeserializer(
            detachedSymbolTable,
            fileReader,
            dummyFileSymbol,
            enqueueLocalTopLevelDeclaration = {},
            irInterner,
            deserializePublicSymbol = ::referencePublicSymbol
        )

        private val declarationDeserializer = IrDeclarationDeserializer(
            irBuiltIns,
            detachedSymbolTable,
            irFactory,
            fileReader,
            parent = dummyFileSymbol.owner,
            allowErrorNodes = false,
            deserializeInlineFunctions = true,
            deserializeBodies = false, // Already covered by `deserializeInlineFunctions = true`.
            symbolDeserializer,
            onDeserializedClass = { _, _ -> },
            needToDeserializeFakeOverrides = { false },
            specialProcessingForMismatchedSymbolKind = null,
            irInterner
        )

        /**
         * Deserialize declarations only on demand. Cache top-level declarations to avoid repetitive deserialization
         * if the declaration happens to have multiple inline functions.
         */
        private val indexWithLazyValues: Map<IdSignature, Lazy<IrDeclaration>> = fileProto.declarationIdList.associate { declarationId ->
            val signature = symbolDeserializer.deserializeIdSignature(declarationId)

            val lazyDeclaration = lazy {
                val declarationProto = fileReader.declaration(declarationId)
                declarationDeserializer.deserializeDeclaration(declarationProto)
            }

            signature to lazyDeclaration
        }

        fun getTopLevelDeclarationOrNull(topLevelSignature: IdSignature): IrDeclaration? = indexWithLazyValues[topLevelSignature]?.value
    }
}
