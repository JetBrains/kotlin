/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.IrDeserializationSettings.DeserializeFunctionBodies
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.LineAndColumn
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.IrArrayReader
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class NonLinkingIrInlineFunctionDeserializer(
    private val irBuiltIns: IrBuiltIns,
    private val signatureComputer: PublicIdSignatureComputer,
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

    private val moduleDeserializers = hashMapOf<KotlinLibrary, ModuleDeserializer?>()
    private val modules = hashMapOf<KotlinLibrary, IrModuleFragment>()

    fun deserializeInlineFunction(function: IrFunction): IrFunction? {
        check(function.isInline) { "Non-inline function: ${function.render()}" }
        check(!function.isFakeOverride) { "Deserialization of fake overrides is not supported: ${function.render()}" }

        if (function.body != null) return null

        check(!function.isEffectivelyPrivate()) { "Deserialization of private inline functions is not supported: ${function.render()}" }

        val deserializedContainerSource = function.containerSource
        check(deserializedContainerSource is KlibDeserializedContainerSource) {
            "Cannot deserialize inline function from a non-Kotlin library: ${function.render()}\nFunction source: " +
                    deserializedContainerSource?.let { "${it::class.java}, ${it.presentableString}" }
        }

        val library = deserializedContainerSource.klib
        val moduleDeserializer = moduleDeserializers.getOrPut(library) {
            when {
                library.hasIrOfInlineableFuns -> ModuleDeserializer(library)
                !library.hasIr -> error("Ir-less library: ${library.libraryFile.path}")
                else -> null
            }
        } ?: return null

        val functionSignature: IdSignature = signatureComputer.computeSignature(function)
        // Inside the module deserializer "functionSignature" will be mapped to erased copy of inline function and this copy will be returned.
        val deserializedFunction: IrFunction =
            moduleDeserializer.deserializeInlineFunction(functionSignature, function.getPackageFragment()) ?: return null
        deserializedFunction.originalOfErasedTopLevelCopy = function
        (deserializedFunction.parent as IrFile).module = modules.getOrPut(library) { IrModuleFragmentImpl(function.module) }
        return deserializedFunction
    }

    private fun referencePublicSymbol(signature: IdSignature, symbolKind: BinarySymbolData.SymbolKind) =
        referenceDeserializedSymbol(detachedSymbolTable, fileSymbol = null, symbolKind, signature)

    private inner class ModuleDeserializer(library: KotlinLibrary) {
        private val fileReader = IrLibraryFileFromBytes(InlinableFunsFileIrKlibBytesSource(library))

        private val dummyFileSymbol = IrFileImpl(
            fileEntry = object : IrFileEntry {
                override val name: String get() = "<dummy>"
                override val maxOffset: Int get() = shouldNotBeCalled()
                override val lineStartOffsets: IntArray get() = shouldNotBeCalled()
                override val firstRelevantLineIndex: Int get() = shouldNotBeCalled()
                override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo = shouldNotBeCalled()
                override fun getLineNumber(offset: Int): Int = shouldNotBeCalled()
                override fun getColumnNumber(offset: Int): Int = shouldNotBeCalled()
                override fun getLineAndColumnNumbers(offset: Int): LineAndColumn = shouldNotBeCalled()
            },
            symbol = IrFileSymbolImpl(),
            packageFqName = FqName("<uninitialized>")
        ).symbol

        private val symbolDeserializer = IrSymbolDeserializer(
            detachedSymbolTable,
            fileReader,
            dummyFileSymbol,
            enqueueLocalTopLevelDeclaration = {},
            irInterner,
            deserializePublicSymbolWithOwnerInUnknownFile = ::referencePublicSymbol
        )

        private val declarationDeserializer = IrDeclarationDeserializer(
            builtIns = irBuiltIns,
            symbolTable = detachedSymbolTable,
            irFactory = irFactory,
            libraryFile = fileReader,
            parent = dummyFileSymbol.owner,
            settings = IrDeserializationSettings(
                deserializeFunctionBodies = DeserializeFunctionBodies.ONLY_INLINE,
                useNullableAnyAsAnnotationConstructorCallType = true,
            ),
            symbolDeserializer = symbolDeserializer,
            onDeserializedClass = { _, _ -> },
            needToDeserializeFakeOverrides = { false },
            specialProcessingForMismatchedSymbolKind = null,
            irInterner = irInterner,
        )

        /**
         * Deserialize declarations only on demand. Cache top-level declarations to avoid repetitive deserialization
         * if the declaration happens to have multiple inline functions.
         */
        private val reversedSignatureIndex: Map<IdSignature, Int> = run {
            val maxSignatureIndex = IrArrayReader(library.signaturesOfInlineableFuns()).entryCount() - 1
            (0..maxSignatureIndex).associateBy { symbolDeserializer.deserializeIdSignature(it) }
        }
        private val deserializedFunctionCache = mutableMapOf<IdSignature, IrFunction?>()

        fun deserializeInlineFunction(signature: IdSignature, originalFunctionPackage: IrPackageFragment): IrFunction? =
            deserializedFunctionCache.getOrPut(signature) {
                val idSigIndex = reversedSignatureIndex[signature] ?: return@getOrPut null
                val functionProto = fileReader.declaration(idSigIndex)
                val function = declarationDeserializer.deserializeDeclaration(functionProto) as IrFunction

                val fileEntryProto = fileReader.fileEntry(functionProto.irFunction.preparedInlineFunctionFileEntryId)!!
                val fileEntry = deserializeFileEntry(fileEntryProto)
                val file = IrFileImpl(
                    symbol = IrFileSymbolImpl(originalFunctionPackage.symbol.descriptor),
                    packageFqName = originalFunctionPackage.packageFqName,
                    fileEntry = fileEntry,
                )

                function.parent = file
                file.declarations += function

                function
            }
    }

    class InlinableFunsFileIrKlibBytesSource(private val klib: IrLibrary) : IrLibraryBytesSource() {
        override fun irDeclaration(index: Int): ByteArray = klib.irDeclarationOfInlineableFuns(index)
        override fun type(index: Int): ByteArray = klib.typeOfInlineableFuns(index)
        override fun signature(index: Int): ByteArray = klib.signatureOfInlineableFuns(index)
        override fun string(index: Int): ByteArray = klib.stringOfInlineableFuns(index)
        override fun body(index: Int): ByteArray = klib.bodyOfInlineableFuns(index)
        override fun debugInfo(index: Int): ByteArray? = klib.debugInfoOfInlineableFuns(index)
        override fun fileEntry(index: Int): ByteArray = klib.fileEntryOfInlineableFuns(index)
    }
}
