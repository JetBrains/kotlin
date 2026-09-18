/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.KlibSignatureIndexConstants.KLIB_EXPORTED_SIGNATURES_INDEX_FILE_NAME
import org.jetbrains.kotlin.backend.common.KlibSignatureIndexConstants.KLIB_IMPORTED_SIGNATURES_INDEX_FILE_NAME
import org.jetbrains.kotlin.backend.common.KlibSignatureIndexConstants.KLIB_INDICES_DIR_NAME
import org.jetbrains.kotlin.backend.common.serialization.IrInterningService
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.KlibComponent
import org.jetbrains.kotlin.library.KlibComponentLayout
import org.jetbrains.kotlin.library.KlibConstants.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.library.KlibLayoutReader
import org.jetbrains.kotlin.library.encodings.WobblyTF8
import org.jetbrains.kotlin.library.impl.IrArrayReader
import org.jetbrains.kotlin.library.impl.IrStringWriter
import org.jetbrains.kotlin.library.writer.KlibComponentWriter
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes
import org.jetbrains.kotlin.backend.common.serialization.proto.CommonIdSignature as ProtoCommonIdSignature

interface KlibSignatureIndexComponent : KlibComponent {

    /** The signatures of only public top-level declarations that belong to the current library. */
    val exportedTopLevelSignatures: Set<IdSignature>

    /**
     * The signatures of public top-level declarations that belong to other libraries,
     * but are referenced/called in the current library. I.e. "imports".
     */
    val importedTopLevelSignatures: Set<IdSignature>

    companion object Kind : KlibComponent.Kind<KlibSignatureIndexComponent, KlibSignatureIndexComponentLayout> {
        override fun createLayout(root: Path) = KlibSignatureIndexComponentLayout(root)

        /**
         * Note: It is expected that every correct Klib has metadata files.
         * Therefore, no data availability check is performed in this method and the component is always created unconditionally.
         */
        override fun createComponentIfDataInKlibIsAvailable(layoutReader: KlibLayoutReader<KlibSignatureIndexComponentLayout>): KlibSignatureIndexComponent? =
            if (layoutReader.readInPlaceOrFallback(false) {
                    it.exportedTopLevelSignaturesFile.isRegularFile() && it.importedTopLevelSignaturesFile.isRegularFile()
                }) KlibSignatureIndexComponentImpl(layoutReader) else null
    }
}

class KlibSignatureIndexComponentWriterImpl(
    private val exportedTopLevelSignatures: Set<IdSignature>,
    private val importedTopLevelSignatures: Set<IdSignature>,
) : KlibComponentWriter {
    override fun writeTo(root: Path) {
        val layout = KlibSignatureIndexComponentLayout(root)
        layout.indicesDir.createDirectories()

        exportedTopLevelSignatures.serializeTo(layout.exportedTopLevelSignaturesFile)
        importedTopLevelSignatures.serializeTo(layout.importedTopLevelSignaturesFile)
    }

    private fun Set<IdSignature>.serializeTo(path: Path) {
        val stringMap = hashMapOf<String, Int>()
        val stringArray = arrayListOf<String>()

        fun serializeString(value: String): Int = stringMap.getOrPut(value) {
            stringArray.add(value)
            stringArray.size - 1
        }

        fun serializeFqName(fqName: String): List<Int> = fqName.split(".").map { serializeString(it) }

        val signatures = mutableListOf<ProtoCommonIdSignature>()
        for (signature in this) {
            if (signature !is IdSignature.CommonSignature) continue // TODO: find out why!

            val proto = ProtoCommonIdSignature.newBuilder()
            proto.addAllPackageFqName(serializeFqName(signature.packageFqName))
            proto.addAllDeclarationFqName(serializeFqName(signature.declarationFqName))

            signature.id?.let { proto.memberUniqId = it }
            if (signature.mask != 0L) { proto.flags = signature.mask }

            signatures += proto.build()
        }

        path.outputStream().use { outputStream ->
            outputStream.write(IrStringWriter(stringArray, useVarInt = true).writeIntoMemory())
            for (signature in signatures) {
                signature.writeDelimitedTo(outputStream)
            }
        }
    }
}

class KlibSignatureIndexComponentLayout(root: Path) : KlibComponentLayout(root) {
    /** The indices' directory. */
    val indicesDir: Path
        get() = root.resolve(KLIB_DEFAULT_COMPONENT_NAME).resolve(KLIB_INDICES_DIR_NAME)

    /** The exported signatures index file. */
    val exportedTopLevelSignaturesFile: Path
        get() = indicesDir.resolve(KLIB_EXPORTED_SIGNATURES_INDEX_FILE_NAME)

    /** The imported signatures index file. */
    val importedTopLevelSignaturesFile: Path
        get() = indicesDir.resolve(KLIB_IMPORTED_SIGNATURES_INDEX_FILE_NAME)
}

private object KlibSignatureIndexConstants {
    const val KLIB_INDICES_DIR_NAME = "indices"
    const val KLIB_EXPORTED_SIGNATURES_INDEX_FILE_NAME = "signatures.exported"
    const val KLIB_IMPORTED_SIGNATURES_INDEX_FILE_NAME = "signatures.imported"
}

private class KlibSignatureIndexComponentImpl(
    private val layoutReader: KlibLayoutReader<KlibSignatureIndexComponentLayout>,
) : KlibSignatureIndexComponent {

    override val exportedTopLevelSignatures: Set<IdSignature>
        get() = layoutReader.readInPlace { it.exportedTopLevelSignaturesFile.deserializeSignatures() }

    override val importedTopLevelSignatures: Set<IdSignature>
        get() = layoutReader.readInPlace { it.importedTopLevelSignaturesFile.deserializeSignatures() }

    companion object {
        private fun Path.deserializeSignatures(): Set<IdSignature> {
            val byteArray = readBytes()

            // Preparations to read names:
            val stringArrayReader = IrArrayReader(byteArray)
            if (stringArrayReader.entryCount() == 0) return emptySet()

            val interner = IrInterningService()
            fun deserializeString(index: Int) = interner.string(WobblyTF8.decode(stringArrayReader.tableItemBytes(index)))

            // Preparations to read serialized signatures:
            val signaturesStartOffset = stringArrayReader.effectiveSize
            val signaturesStream = byteArray.inputStream(signaturesStartOffset, byteArray.size - signaturesStartOffset)

            // Read signatures
            val result = hashSetOf<IdSignature>()
            while (signaturesStream.available() > 0) {
                val proto = ProtoCommonIdSignature.parseDelimitedFrom(signaturesStream) ?: break

                val pkg = proto.packageFqNameList.joinToString(separator = ".", transform = ::deserializeString)
                val cls = proto.declarationFqNameList.joinToString(separator = ".", transform = ::deserializeString)

                val memberId = when {
                    proto.hasMemberUniqId() -> proto.memberUniqId
                    proto.hasMemberUniqIdPre240() -> proto.memberUniqIdPre240
                    else -> null
                }

                result += IdSignature.CommonSignature(
                    packageFqName = pkg,
                    declarationFqName = cls,
                    id = memberId,
                    mask = proto.flags,
                    description = null,
                )
            }

            return result
        }
    }
}
