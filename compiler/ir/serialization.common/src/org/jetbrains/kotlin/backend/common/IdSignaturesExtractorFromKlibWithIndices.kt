/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.IdSignaturesExtractor.ExtractedSignatures
import org.jetbrains.kotlin.library.KlibLayoutReader
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import java.nio.file.Path

/**
 * This is an implementation of [IdSignaturesExtractor] that allows reading top-level signatures from
 * the library's [KlibSignatureIndexComponent] component. Or, if the component is missing, delegated to
 * the [delegate] signature extractor.
 */
class IdSignaturesExtractorFromKlibWithIndices(
    private val library: KotlinLibrary,
    private val delegate: IdSignaturesExtractor,
    private val externalIndicesParameters: ExternalKlibSignatureIndicesParameters? = null,
) : IdSignaturesExtractor {
    override fun extractAllPublicSignatures() = delegate.extractAllPublicSignatures()

    override fun extractOnlyTopLevelPublicSignatures() = getExtractedSignaturesFromIndexIfPossible()
        ?: delegate.extractOnlyTopLevelPublicSignatures()

    private fun getExtractedSignaturesFromIndexIfPossible(): ExtractedSignatures? {
        // If there is an index inside the library, return signatures from it.
        library.signatureIndex?.let { return it.toExtractedSignatures() }

        // If there are no conditions under which an external index for the current library can be generated,
        // return `null` to fall back to computing `ExtractedSignatures` on the fly using `delegate`.
        if (externalIndicesParameters == null ||
            externalIndicesParameters.pathPrefixesForGenerationSignatureIndices.none { library.canonicalPath.startsWith(it) }
        ) {
            return null
        }

        // If there is the external index, return signatures from it.
        KlibSignatureIndexComponent.createComponentIfDataInKlibIsAvailable(
            KlibLayoutReader.FromDirectory(
                externalIndicesParameters.externalSignatureIndicesDir,
                ::getExternalIndexLayout
            )
        )?.let { return it.toExtractedSignatures() }

        // Else, compute signatures and store them as the external index on the file system.
        val [exported, imported] = delegate.extractOnlyTopLevelPublicSignatures()
        KlibSignatureIndexComponentWriterImpl(
            exportedTopLevelSignatures = exported,
            importedTopLevelSignatures = imported,
            layoutBuilder = ::getExternalIndexLayout,
        ).writeTo(externalIndicesParameters.externalSignatureIndicesDir)
        return ExtractedSignatures(exported, imported)
    }

    private fun getExternalIndexLayout(root: Path) = KlibSignatureIndexComponentLayout.ExternalIndex(
        root = root,
        libraryName = library.uniqueName,
        targetDiscriminator = externalIndicesParameters!!.targetDiscriminator,
        libraryFingerprintHash = library.lazyEvaluatedFingerprintHash,
    )

    companion object {
        private fun KlibSignatureIndexComponent.toExtractedSignatures() = ExtractedSignatures(
            declaredSignatures = exportedTopLevelSignatures,
            importedSignatures = importedTopLevelSignatures,
        )
    }
}
