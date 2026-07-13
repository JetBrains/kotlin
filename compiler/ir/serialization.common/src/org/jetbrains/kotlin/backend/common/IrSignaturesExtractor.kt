/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFileFromBytes.Companion.extensionRegistryLite
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration.DeclaratorCase.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IdSignature.AccessorSignature
import org.jetbrains.kotlin.ir.util.IdSignature.CommonSignature
import org.jetbrains.kotlin.ir.util.IdSignature.CompositeSignature
import org.jetbrains.kotlin.ir.util.IdSignature.FileSignature
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.KlibIrComponentLayout
import org.jetbrains.kotlin.library.components.irOrFail
import org.jetbrains.kotlin.library.impl.IrArrayReader
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase as ProtoDeclarationBase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrClass as ProtoClass
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeAlias as ProtoTypeAlias
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionBase as ProtoFunctionBase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrProperty as ProtoProperty

/**
 * This is a lightweight tool that allows extracting [IdSignature]s from the given [KotlinLibrary].
 *
 * The tool is supposed to be robust. It does not deserialize IR tree nodes and run the IR linkage like the compiler does.
 * Instead, it does few targeted IO reads and a limited number of deserialization calls to access IR proto stubs:
 * - [KlibIrComponentLayout.signaturesFile] is read to load the full set of [IdSignature]s that are used in the current library.
 * - [KlibIrComponentLayout.irFilesFile] is read to get the list of top-level public declarations in each library's file.
 * - [KlibIrComponentLayout.declarationsFile] is read to explore what are the nested/member public declarations of the above-mentioned
 *   top-level declarations.
 *
 * There are two entry points:
 * - [extractAllPublicSignatures] extracts signatures of both top-level and nested/member public declarations.
 *   This endpoint is useful for running investigations, debugging, etc.
 * - [extractOnlyTopLevelPublicSignatures] extracts signatures of only top-level public declarations.
 *   This endpoint can be helpful for e.g. understanding the DAG of dependencies between libraries.
 *
 * Note: [extractOnlyTopLevelPublicSignatures] is written in way to do even lesser amount of IO reads. So, it's
 * supposed to be even more robust than [extractAllPublicSignatures].
 */
class IrSignaturesExtractor(library: KotlinLibrary) {
    private val interner = IrInterningService()
    private val ir = library.irOrFail

    /**
     * The two sets of extracted signatures:
     *
     * @property declaredSignatures The signatures of public declarations that belong to the current library.
     * @property importedSignatures The signatures of public declarations that belong to other libraries,
     *   but are referenced/called in the current library. I.e. "imports".
     */
    data class ExtractedSignatures(
        val declaredSignatures: Set<IdSignature>,
        val importedSignatures: Set<IdSignature>,
    )

    private inner class IrSignatureExtractorFromFile(
        private val fileIndex: Int,
    ) {
        private val fileProto = ProtoFile.parseFrom(ir.irFile(fileIndex).codedInputStream, extensionRegistryLite)
        private val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(ir, fileIndex))

        private val signatureDeserializer = IdSignatureDeserializer(
            libraryFile = fileReader,
            fileSignature = FileSignature(
                id = Any(), // Just any unique object.
                fqName = FqName("<dummy>"), // Just a dummy FQ name.
                fileName = "<dummy>", // Just a dummy file name.
            ),
            irInterner = interner,
        )

        /**
         * Extract the full set of [IdSignature]s that are used in the current file. This includes both
         * the signatures of declarations that belong to the current file and the signatures of declarations
         * from other files/libraries that are just referenced/used in the current file.
         */
        fun extractSignaturesOfAllKnownPublicDeclarations(): Set<IdSignature> {
            val result = hashSetOf<IdSignature>()
            val innerSignaturesOfPrivateSymbols = hashSetOf<IdSignature>()

            val signatureCount = IrArrayReader(ir.signatures(fileIndex)).entryCount()
            for (index in 0 until signatureCount) {
                when (val signature = signatureDeserializer.deserializeIdSignature(index)) {
                    is CommonSignature, is AccessorSignature -> {
                        /** Only [CommonSignature] and [AccessorSignature] can represent public declarations. */
                        result += signature
                    }
                    is CompositeSignature if signature.container is FileSignature -> {
                        /**
                         * Private declarations have a [CompositeSignature] where the [CompositeSignature.container] part is
                         * an instance of [FileSignature] and [CompositeSignature.inner] part is an instance of
                         * either [CommonSignature] or [AccessorSignature]. When such a signature serialized to KLIB, three
                         * distinct records are added to [KlibIrComponentLayout.signaturesFile]:
                         * - A record for the [CompositeSignature] itself
                         * - A record for [FileSignature]
                         * - And a record for the inner part, i.e. [CommonSignature] or [AccessorSignature]
                         *
                         * To avoid accidentally counting the latter type of records as "a signature of a public declaration",
                         * we need to track them in a separate set ([innerSignaturesOfPrivateSymbols]) that later will be extracted
                         * from the resulting set of signatures ([result]).
                         */
                        innerSignaturesOfPrivateSymbols += signature.inner
                    }
                    else -> {
                        /* We are not interested in any other kinds of signatures */
                    }
                }
            }

            result -= innerSignaturesOfPrivateSymbols

            return result
        }

        /**
         * Extract [IdSignature]s of all public declarations that belong to the current file.
         */
        fun extractSignaturesOfOwnPublicDeclarations(): Set<IdSignature> {
            val result = hashSetOf<IdSignature>()

            for (topLevelDeclarationIndex in fileProto.declarationIdList) {
                extractSignature(declarationProto = fileReader.declaration(topLevelDeclarationIndex), result)
            }

            return result
        }

        /**
         * Extract [IdSignature]s of all top-level public declarations that belong to the current file.
         */
        fun extractSignaturesOfOwnTopLevelPublicDeclarations(): Set<IdSignature> {
            val result = hashSetOf<IdSignature>()

            for (topLevelDeclarationIndex in fileProto.declarationIdList) {
                val signature = signatureDeserializer.deserializeIdSignature(topLevelDeclarationIndex)
                if (signature is CommonSignature) result += signature
            }

            return result
        }

        private fun extractSignature(declarationProto: ProtoDeclaration, output: MutableSet<IdSignature>) {
            when (declarationProto.declaratorCase) {
                IR_CLASS -> extractSignatureFromClass(declarationProto.irClass, output)
                IR_CONSTRUCTOR -> extractSignatureFromFunction(declarationProto.irConstructor.base, output)
                IR_FUNCTION -> extractSignatureFromFunction(declarationProto.irFunction.base, output)
                IR_PROPERTY -> extractSignatureFromProperty(declarationProto.irProperty, output)
                IR_TYPE_ALIAS -> extractSignatureFromTypeAlias(declarationProto.irTypeAlias, output)
                IR_ENUM_ENTRY -> extractSignatureFromSymbol(declarationProto.irEnumEntry.base.symbol, output)
                IR_ANONYMOUS_INIT,
                IR_TYPE_PARAMETER,
                IR_FIELD,
                IR_VARIABLE,
                IR_VALUE_PARAMETER,
                IR_LOCAL_DELEGATED_PROPERTY,
                DECLARATOR_NOT_SET, null -> {
                    /* We are not interested in these kinds of declaraions, because they all are effectively private. */
                }
            }
        }

        private fun extractSignatureFromClass(classProto: ProtoClass, output: MutableSet<IdSignature>) {
            if (classProto.base.isPrivate()) return
            extractSignatureFromSymbol(classProto.base.symbol, output)
            classProto.declarationList.forEach { extractSignature(it, output) }
        }

        private fun extractSignatureFromFunction(functionProto: ProtoFunctionBase, output: MutableSet<IdSignature>) {
            if (functionProto.base.isPrivate()) return
            extractSignatureFromSymbol(functionProto.base.symbol, output)
        }

        private fun extractSignatureFromProperty(propertyProto: ProtoProperty, output: MutableSet<IdSignature>) {
            if (propertyProto.base.isPrivate()) return
            extractSignatureFromSymbol(propertyProto.base.symbol, output)
            propertyProto.hasGetter().ifTrue { extractSignatureFromFunction(propertyProto.getter.base, output) }
            propertyProto.hasSetter().ifTrue { extractSignatureFromFunction(propertyProto.setter.base, output) }
        }

        private fun extractSignatureFromTypeAlias(typeAliasProto: ProtoTypeAlias, output: MutableSet<IdSignature>) {
            if (typeAliasProto.base.isPrivate()) return
            extractSignatureFromSymbol(typeAliasProto.base.symbol, output)
        }

        private fun extractSignatureFromSymbol(symbolId: Long, output: MutableSet<IdSignature>) {
            val signatureId = BinarySymbolData.decode(symbolId).signatureId
            val signature = signatureDeserializer.deserializeIdSignature(signatureId)
            output += signature
        }

        private fun ProtoDeclarationBase.isPrivate(): Boolean =
            when (IrFlags.VISIBILITY.get(flags.toInt())) {
                ProtoBuf.Visibility.PUBLIC,
                ProtoBuf.Visibility.PROTECTED,
                ProtoBuf.Visibility.INTERNAL -> false
                ProtoBuf.Visibility.PRIVATE,
                ProtoBuf.Visibility.PRIVATE_TO_THIS,
                ProtoBuf.Visibility.LOCAL,
                null -> true
            }
    }

    /**
     * Extracts signatures of both top-level and nested/member public declarations.
     *
     * Note: This endpoint is useful when we need to see all contents of a KLIB.
     * E.g. for running investigations, debugging, etc.
     */
    fun extractAllPublicSignatures(): ExtractedSignatures {
        val allKnownSignatures: MutableSet<IdSignature> = hashSetOf()
        val ownDeclarationSignatures: MutableSet<IdSignature> = hashSetOf()

        for (fileIndex in 0 until ir.irFileCount) {
            val extractorFromFile = IrSignatureExtractorFromFile(fileIndex)
            allKnownSignatures += extractorFromFile.extractSignaturesOfAllKnownPublicDeclarations()
            ownDeclarationSignatures += extractorFromFile.extractSignaturesOfOwnPublicDeclarations()
        }

        val importedSignatures = allKnownSignatures.filterTo(hashSetOf()) { signature ->
            signature.topLevelSignature() !in ownDeclarationSignatures
        }

        return ExtractedSignatures(
            declaredSignatures = ownDeclarationSignatures,
            importedSignatures = importedSignatures,
        )
    }

    /**
     * Extracts signatures of only top-level public declarations.
     *
     * Note: This endpoint can be helpful for e.g. understanding the DAG of dependencies between libraries.
     * Also, it is written in way to do even lesser amount of IO reads than [extractAllPublicSignatures] for even better performance.
     */
    fun extractOnlyTopLevelPublicSignatures(): ExtractedSignatures {
        val allKnownSignatures: MutableSet<IdSignature> = hashSetOf()
        val ownDeclarationSignatures: MutableSet<IdSignature> = hashSetOf()

        for (fileIndex in 0 until ir.irFileCount) {
            val extractorFromFile = IrSignatureExtractorFromFile(fileIndex)
            extractorFromFile.extractSignaturesOfAllKnownPublicDeclarations().forEach {
                allKnownSignatures += it.topLevelSignature()
            }

            ownDeclarationSignatures += extractorFromFile.extractSignaturesOfOwnTopLevelPublicDeclarations()
        }

        val importedSignatures = allKnownSignatures.filterTo(hashSetOf()) { signature ->
            signature !in ownDeclarationSignatures
        }

        return ExtractedSignatures(
            declaredSignatures = ownDeclarationSignatures,
            importedSignatures = importedSignatures,
        )
    }
}
