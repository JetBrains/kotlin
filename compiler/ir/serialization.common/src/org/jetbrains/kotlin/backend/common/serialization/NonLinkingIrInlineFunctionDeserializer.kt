/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import jdk.nashorn.internal.objects.NativeFunction.function
import org.jetbrains.kotlin.backend.common.serialization.IrDeserializationSettings.DeserializeFunctionBodies
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.encodings.FunctionFlags
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration.DeclaratorCase.IR_CLASS
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration.DeclaratorCase.IR_FUNCTION
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as IrDeclarationProto

class NonLinkingIrInlineFunctionDeserializer(
    private val irBuiltIns: IrBuiltIns,
    private val signatureComputer: PublicIdSignatureComputer,
) {
    private val irInterner = IrInterningService()

    /**
     * This is a separate symbol table ("detached") from the symbol table ("main") that is used in IR linker.
     *
     * The goal is to separate the linkage process, which should end with all symbols been bound to the respective declarations,
     * and the process of partial deserialization of inline functions, which should produce some amount of unbound symbols
     * that are not supposed to be linked and therefore should not be tracked in the main symbol table.
     */
    private val detachedSymbolTable = SymbolTable(signaturer = null, IrFactoryImpl)

    private val moduleDeserializers = hashMapOf<KotlinLibrary, ModuleDeserializer>()

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
        val moduleDeserializer = moduleDeserializers.getOrPut(library) { ModuleDeserializer(library) }

        val functionSignature = function.symbol.signature ?: signatureComputer.computeSignature(function)
        val deserializedFunction = moduleDeserializer.deserializeFunctionOrNull(functionSignature) ?: return null
        return deserializedFunction
    }

    private inner class ModuleDeserializer(library: KotlinLibrary) {
        init {
            check(library.hasIr) { "Ir-less library: ${library.libraryFile.path}" }
        }

        private val fileDeserializers = (0 until library.fileCount()).map { fileIndex ->
            FileDeserializer(this, library, fileIndex)
        }

        fun deserializeFunctionOrNull(signature: IdSignature): IrFunction? =
            fileDeserializers.firstNotNullOfOrNull {
                it.deserializeFunction(signature)
            }
    }

    private inner class FileDeserializer(private val moduleDeserializer: ModuleDeserializer, library: KotlinLibrary, fileIndex: Int) {
        private val fileProto = ProtoFile.parseFrom(library.file(fileIndex).codedInputStream, ExtensionRegistryLite.newInstance())
        private val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(library, fileIndex))

        private val dummyFileSymbol = IrFileSymbolImpl().apply {
            val fileEntry = library.fileEntry(fileProto, fileIndex)
            IrFileImpl(
                fileEntry = NaiveSourceBasedFileEntryImpl(fileEntry.name, fileEntry.lineStartOffsetList.toIntArray()),
                symbol = this,
                packageFqName = FqName(irInterner.string(fileReader.deserializeFqName(fileProto.fqNameList)))
            )
        }

        val symbolDeserializer = IrSymbolDeserializer(
            detachedSymbolTable,
            fileReader,
            dummyFileSymbol,
            enqueueLocalTopLevelDeclaration = {},
            irInterner,
            deserializePublicSymbolWithOwnerInUnknownFile = { sig, kind ->
                if (kind == BinarySymbolData.SymbolKind.FUNCTION_SYMBOL) {
                    moduleDeserializer.deserializeFunctionOrNull(sig)?.symbol?.let {
                        return@IrSymbolDeserializer it
                    }
                }
                referenceDeserializedSymbol(detachedSymbolTable, fileSymbol = null, kind, sig)
            }
        )

        private val declarationDeserializer = IrDeclarationDeserializer(
            builtIns = irBuiltIns,
            symbolTable = detachedSymbolTable,
            irFactory = IrFactoryImpl,
            libraryFile = fileReader,
            parent = dummyFileSymbol.owner,
            settings = IrDeserializationSettings(
                deserializeFunctionBodies = DeserializeFunctionBodies.ONLY_INLINE,
                useNullableAnyAsAnnotationConstructorCallType = true,
                allowAlreadyBoundSymbols = true,
            ),
            symbolDeserializer = symbolDeserializer,
            onDeserializedClass = { _, _ -> },
            needToDeserializeFakeOverrides = { false },
            specialProcessingForMismatchedSymbolKind = null,
            irInterner = irInterner,
        )

        private val reversedSignatureIndex = fileProto.declarationIdList.associateBy { symbolDeserializer.deserializeIdSignature(it) }

        fun deserializeFunction(signature: IdSignature): IrFunction? {
            val topLevelSignature = signature.topLevelSignature()
            val topLevelDeclarationIndex = reversedSignatureIndex[topLevelSignature] ?: return null
            val topLevelDeclarationProto = fileReader.declaration(topLevelDeclarationIndex)
            return findFunctionInFileOrClass(topLevelDeclarationProto, signature)
        }

        private fun findFunctionInFileOrClass(declarationProto: IrDeclarationProto, signature: IdSignature): IrFunction? {
            when (declarationProto.declaratorCase!!) {
                IR_FUNCTION -> {
                    val symbolIndex = declarationProto.irFunction.base.base.symbol
                    val symbolData = symbolDeserializer.parseSymbolData(symbolIndex)
                    val memberSignature = symbolDeserializer.deserializeIdSignature(symbolData.signatureId)
                    if (memberSignature == signature) {
                        if (memberSignature !is IdSignature.CompositeSignature) {
                            val flags = FunctionFlags.decode(declarationProto.irFunction.base.base.flags)
                            if (!flags.isInline) {
                                return null
                            }
                        }

                        val symbol = symbolDeserializer.deserializeSymbolToDeclareInCurrentFile(symbolIndex).first
                        if (!symbol.isBound) {
                            declarationDeserializer.deserializeDeclaration(declarationProto, setParent = false) as IrFunction
                        }
                        val function = symbol.owner as IrFunction
                        function.isDeserializedFromOtherModule = true
                        return function
                    }
                }
                IR_CLASS -> {
                    for (declProto in declarationProto.irClass.declarationList) {
                        findFunctionInFileOrClass(declProto, signature)?.let {
                            return it
                        }
                    }
                }
                else -> {}
            }

            return null
        }
    }
}

var IrFunction.isDeserializedFromOtherModule by irFlag(copyByDefault = true)