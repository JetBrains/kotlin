/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.IdSignaturesExtractor.ExtractedSignatures
import org.jetbrains.kotlin.backend.common.KlibSignatureIndexConstants.KLIB_INDICES_DIR_NAME
import org.jetbrains.kotlin.backend.common.KlibSignatureIndexConstants.KLIB_SIGNATURE_INDEX_FILE_NAME
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.KlibConstants.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.library.writer.KlibComponentWriter
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.KlibWriterSpec
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes

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
        override fun createLayout(root: Path) = KlibSignatureIndexComponentLayout(root)

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
) : KlibComponentWriter {
    constructor(
        exportedTopLevelSignatures: Set<IdSignature>,
        importedTopLevelSignatures: Set<IdSignature>,
    ) : this(
        serializeSignaturesIndex(
            exportedSignatures = exportedTopLevelSignatures,
            importedSignatures = importedTopLevelSignatures,
        ).let(::SerializedSignatureIndex)
    )

    override fun writeTo(root: Path) {
        val layout = KlibSignatureIndexComponentLayout(root)
        layout.indicesDir.createDirectories()
        layout.signatureIndexFile.outputStream().use { it.write(serializedSignatureIndex.signatureIndex) }
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

class KlibSignatureIndexComponentLayout(root: Path) : KlibComponentLayout(root) {
    /** The indices' directory. */
    val indicesDir: Path
        get() = root.resolve(KLIB_DEFAULT_COMPONENT_NAME).resolve(KLIB_INDICES_DIR_NAME)

    /** The file with the signature index. */
    val signatureIndexFile: Path
        get() = indicesDir.resolve(KLIB_SIGNATURE_INDEX_FILE_NAME)
}

private object KlibSignatureIndexConstants {
    const val KLIB_INDICES_DIR_NAME = "indices"
    const val KLIB_SIGNATURE_INDEX_FILE_NAME = "signatures.idx"
}

private class KlibSignatureIndexComponentImpl(
    private val layoutReader: KlibLayoutReader<KlibSignatureIndexComponentLayout>,
) : KlibSignatureIndexComponent {

    private val signatures: Pair<Set<IdSignature>, Set<IdSignature>> by lazy {
        layoutReader.readInPlace { deserializeSignaturesIndex(it.signatureIndexFile.readBytes()) }
    }

    override val exportedTopLevelSignatures: Set<IdSignature>
        get() = signatures.first

    override val importedTopLevelSignatures: Set<IdSignature>
        get() = signatures.second

}
