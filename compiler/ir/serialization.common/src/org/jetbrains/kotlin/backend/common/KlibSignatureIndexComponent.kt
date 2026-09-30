/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.IdSignaturesExtractor.ExtractedSignatures
import org.jetbrains.kotlin.backend.common.KlibSignatureIndexConstants.KLIB_SIGNATURE_INDEX_FILE_NAME
import org.jetbrains.kotlin.backend.common.KlibSignatureIndexConstants.KLIB_INDICES_DIR_NAME
import org.jetbrains.kotlin.backend.common.serialization.CommonSignatureSerializer
import org.jetbrains.kotlin.backend.common.serialization.FingerprintHash
import org.jetbrains.kotlin.backend.common.serialization.IrStringSerializer
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IdSignatureRenderer
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.library.Klib
import org.jetbrains.kotlin.library.KlibComponent
import org.jetbrains.kotlin.library.KlibComponentLayout
import org.jetbrains.kotlin.library.KlibConstants.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.library.KlibLayoutReader
import org.jetbrains.kotlin.library.SerializedSignatureIndex
import org.jetbrains.kotlin.library.encodings.WobblyTF8
import org.jetbrains.kotlin.library.impl.IrArrayWriter
import org.jetbrains.kotlin.library.impl.IrMultiArrayReader
import org.jetbrains.kotlin.library.writer.KlibComponentWriter
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.KlibWriterSpec
import org.jetbrains.kotlin.utils.newHashSetWithExpectedSize
import java.nio.file.Path
import kotlin.collections.joinToString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes
import org.jetbrains.kotlin.backend.common.serialization.proto.CommonIdSignature as ProtoCommonIdSignature

/**
 * A special component that provides access to the IR signature index.
 */
interface KlibSignatureIndexComponent : KlibComponent {

    /** The signatures of only public top-level declarations that belong to the current library. */
    val exportedTopLevelSignatures: Set<IdSignature>

    /**
     * The signatures of public top-level declarations that belong to other libraries,
     * but are referenced/called in the current library. I.e. "imports".
     */
    val importedTopLevelSignatures: Set<IdSignature>

    companion object Kind : KlibComponent.Kind<KlibSignatureIndexComponent, KlibSignatureIndexComponentLayout> {
        override fun createLayout(root: Path) = KlibSignatureIndexComponentLayout.IndexInLibrary(root)

        /**
         * Note: It is expected that every correct Klib has metadata files.
         * Therefore, no data availability check is performed in this method and the component is always created unconditionally.
         */
        override fun createComponentIfDataInKlibIsAvailable(layoutReader: KlibLayoutReader<KlibSignatureIndexComponentLayout>): KlibSignatureIndexComponent? =
            if (layoutReader.readInPlaceOrFallback(false) { it.signatureIndexFile.exists() }) KlibSignatureIndexComponentImpl(layoutReader) else null
    }
}

/**
 * A shortcut for accessing the [KlibSignatureIndexComponent] responsible for signature index in the [Klib] instance.
 *
 * This component is optional: The [signatureIndex] getter returns `null` if there is index in the library.
 */
inline val Klib.signatureIndex: KlibSignatureIndexComponent?
    get() = getComponent(KlibSignatureIndexComponent.Kind)

/**
 * A special component writer that allows writing the IR signature index to the file system.
 */
class KlibSignatureIndexComponentWriterImpl(
    val serializedSignatureIndex: SerializedSignatureIndex,
    private val layoutBuilder: (root: Path) -> KlibSignatureIndexComponentLayout = KlibSignatureIndexComponentLayout::IndexInLibrary,
) : KlibComponentWriter {
    constructor(
        exportedTopLevelSignatures: Set<IdSignature>,
        importedTopLevelSignatures: Set<IdSignature>,
        layoutBuilder: (root: Path) -> KlibSignatureIndexComponentLayout = KlibSignatureIndexComponentLayout::IndexInLibrary,
    ) : this(
        serializeSignatures(
            exportedSignatures = exportedTopLevelSignatures,
            importedSignatures = importedTopLevelSignatures,
        ).writeIntoMemory().let(::SerializedSignatureIndex),
        layoutBuilder,
    )

    override fun writeTo(root: Path) {
        val layout = layoutBuilder(root)
        layout.indicesDir.createDirectories()
        layout.signatureIndexFile.outputStream().use { it.write(serializedSignatureIndex.signatureIndex) }
    }

    companion object {
        private fun serializeSignatures(
            exportedSignatures: Set<IdSignature>,
            importedSignatures: Set<IdSignature>,
        ): IrArrayWriter {
            val stringSerializer = IrStringSerializer()
            val signatureSerializer = CommonSignatureSerializer(stringSerializer, debugInfoSerializer = null)

            fun serializeSignatures(unorderedSignatures: Set<IdSignature>): List<ByteArray> {
                if (unorderedSignatures.isEmpty()) return emptyList()

                val orderedSignatures = ArrayList<Pair<String, IdSignature.CommonSignature>>(unorderedSignatures.size)

                unorderedSignatures.mapTo(orderedSignatures) { signature ->
                    check(signature is IdSignature.CommonSignature) {
                        "Unexpected signature type: ${signature.javaClass.name}, $signature"
                    }

                    val sortingKey = signature.render(IdSignatureRenderer.DEFAULT)
                    sortingKey to signature
                }

                orderedSignatures.sortBy { it.first }

                return orderedSignatures.map { signatureSerializer.serializeSignature(it.second).toByteArray() }
            }

            val serializedExportedSignatures = serializeSignatures(exportedSignatures)
            val serializedImportedSignatures = serializeSignatures(importedSignatures)

            return IrArrayWriter(
                listOf(
                    stringSerializer.toIrStringWriter(useVarIntInDataArrays = true).writeIntoMemory(),
                    IrArrayWriter(serializedExportedSignatures, useVarInt = true).writeIntoMemory(),
                    IrArrayWriter(serializedImportedSignatures, useVarInt = true).writeIntoMemory(),
                ),
                useVarInt = true,
            )
        }
    }
}

/**
 * A [KlibWriter] DSL extension to include [KlibSignatureIndexComponent] to the created library.
 */
fun KlibWriterSpec.includeSignatureIndex(topLevelSignatures: ExtractedSignatures) {
    include(
        KlibSignatureIndexComponentWriterImpl(
            exportedTopLevelSignatures = topLevelSignatures.declaredSignatures,
            importedTopLevelSignatures = topLevelSignatures.importedSignatures,
        )
    )
}

/**
 * A [KlibWriter] DSL extension to include [KlibSignatureIndexComponent] to the created library.
 */
fun KlibWriterSpec.includeSignatureIndex(serializedSignatureIndex: SerializedSignatureIndex?) {
    include(
        KlibSignatureIndexComponentWriterImpl(serializedSignatureIndex ?: return)
    )
}

sealed class KlibSignatureIndexComponentLayout(root: Path) : KlibComponentLayout(root) {
    /** The indices' directory. */
    abstract val indicesDir: Path

    /** The file with the signature index. */
    val signatureIndexFile: Path
        get() = indicesDir.resolve(KLIB_SIGNATURE_INDEX_FILE_NAME)

    /**
     * The layout for the index stored inside the library (aka "own" index).
     */
    class IndexInLibrary(root: Path) : KlibSignatureIndexComponentLayout(root) {
        override val indicesDir: Path
            get() = root.resolve(KLIB_DEFAULT_COMPONENT_NAME).resolve(KLIB_INDICES_DIR_NAME)
    }

    /**
     * The layout for the index stored outside the library (aka "external" index).
     *
     * Note: There is currently a limitation that a library must have exactly one target, which will be used as an additional
     * discriminator for storing the external index on the file system. That's important, because often libraries have group/artifact ID
     * included into their `unique_name` property (which is passed as [libraryName]), but not the target name. For example,
     * a library  with `org.jetbrains.kotlinx:kotlinx-coroutines-core-macosarm64:1.7.2` Maven coordinates has
     * `unique_name=org.jetbrains.kotlinx:kotlinx-coroutines-core` in manifest. So, having external indices for several variants of
     * the same library for different targets will end up in more records inside the same single index directory, which might cause
     * more difficulties for investigating issues and maintaining.
     *
     * This limitation can be lifted in the future, if necessary.
     *
     * See also [ExternalKlibSignatureIndicesParameters].
     */
    class ExternalIndex(
        root: Path,
        private val libraryName: String,
        private val targetDiscriminator: String,
        private val libraryFingerprintHash: FingerprintHash,
    ) : KlibSignatureIndexComponentLayout(root) {
        override val indicesDir: Path
            get() = root.resolve(targetDiscriminator).resolve(libraryName).resolve(libraryFingerprintHash.toString())
    }
}

/**
 * The necessary parameters to set up the generation and use of external signature indices.
 *
 * Note: There is currently a limitation that a library must have exactly one target, which will be used as an additional
 * discriminator for storing the external index on the file system. That's important, because often libraries have group/artifact ID
 * included into their `unique_name` property (which is passed as [targetDiscriminator]), but not the target name. For example,
 * a library  with `org.jetbrains.kotlinx:kotlinx-coroutines-core-macosarm64:1.7.2` Maven coordinates has
 * `unique_name=org.jetbrains.kotlinx:kotlinx-coroutines-core` in manifest. So, having external indices for several variants of
 * the same library for different targets will end up in more records inside the same single index directory, which might cause
 * more difficulties for investigating issues and maintaining.
 *
 * This limitation can be lifted in the future, if necessary.
 *
 * See also [KlibSignatureIndexComponentLayout.ExternalIndex].
 */
class ExternalKlibSignatureIndicesParameters(
    val targetDiscriminator: String,
    val externalSignatureIndicesDir: Path,
    val pathPrefixesForGenerationSignatureIndices: List<Path>,
)

object KlibSignatureIndexConstants {
    const val KLIB_INDICES_DIR_NAME = "indices"
    const val KLIB_SIGNATURE_INDEX_FILE_NAME = "signatures.idx"
}

private class KlibSignatureIndexComponentImpl(
    private val layoutReader: KlibLayoutReader<KlibSignatureIndexComponentLayout>,
) : KlibSignatureIndexComponent {

    private val signatures: Pair<Set<IdSignature>, Set<IdSignature>> by lazy {
        layoutReader.readInPlace { deserializeSignatures(it.signatureIndexFile) }
    }

    override val exportedTopLevelSignatures: Set<IdSignature>
        get() = signatures.first

    override val importedTopLevelSignatures: Set<IdSignature>
        get() = signatures.second

    companion object {
        private fun deserializeSignatures(signatureIndexFile: Path): Pair<Set<IdSignature>, Set<IdSignature>> {
            val multiReader = IrMultiArrayReader(signatureIndexFile.readBytes())

            when (val rowCount = multiReader.rowCount()) {
                in 0..2 -> error("Unexpected row count in signature index: $rowCount")
                in 3..Int.MAX_VALUE -> {
                    // OK. The number of arrays greater than the current compiler expects means we are dealing with
                    // the future version of the index, which has more entries with some extra data in them.
                    // We just wouldn't read that data.
                }
            }

            val stringArray = multiReader.deserializeStringArray()
            val exportedSignatures = multiReader.deserializeSignatures(rowIndex = 1, stringArray)
            val importedSignatures = multiReader.deserializeSignatures(rowIndex = 2, stringArray)

            return exportedSignatures to importedSignatures
        }

        private fun IrMultiArrayReader.deserializeStringArray(): Array<out String> {
            return Array(columnCount(0)) { index ->
                WobblyTF8.decode(tableItemBytes(rowIndex = 0, columnIndex = index))
            }
        }

        private fun IrMultiArrayReader.deserializeSignatures(
            rowIndex: Int,
            stringArray: Array<out String>,
        ): Set<IdSignature> {
            val size = columnCount(rowIndex)
            val result = newHashSetWithExpectedSize<IdSignature>(size)

            for (index in 0 until size) {
                val proto = ProtoCommonIdSignature.parseFrom(tableItemBytes(rowIndex = rowIndex, columnIndex = index))

                result += IdSignature.CommonSignature(
                    packageFqName = proto.packageFqNameList.joinToString(separator = ".", transform = stringArray::get),
                    declarationFqName = proto.declarationFqNameList.joinToString(separator = ".", transform = stringArray::get),
                    id = when {
                        proto.hasMemberUniqId() -> proto.memberUniqId
                        proto.hasMemberUniqIdPre240() -> proto.memberUniqIdPre240
                        else -> null
                    },
                    mask = proto.flags,
                    description = null,
                )
            }

            return result
        }
    }
}
