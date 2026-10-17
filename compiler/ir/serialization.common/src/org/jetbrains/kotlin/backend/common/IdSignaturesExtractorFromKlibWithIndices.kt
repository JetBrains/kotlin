/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.IdSignaturesExtractor.ExtractedSignatures
import org.jetbrains.kotlin.io.withExclusiveFileLock
import org.jetbrains.kotlin.library.KlibLayoutReader
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.time.Duration.Companion.milliseconds

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

        // Otherwise, let's try to access an external index.
        val externalIndexLayout = getExternalIndexLayoutIfPossible() ?: return null

        // The external index can be concurrently read or written by other threads or processes.
        // So, access it only under the lock.
        return withExclusiveFileLock(externalIndexLayout.lockFile, retryTimeout = 20.milliseconds) {
            getExtractedSignaturesFromExternalIndex(externalIndexLayout)
        }
    }

    private fun getExternalIndexLayoutIfPossible(): KlibSignatureIndexComponentLayout.ExternalIndex? {
        // If there are no conditions under which an external index for the current library can be generated,
        // return `null` to fall back to computing `ExtractedSignatures` on the fly using `delegate`.
        if (externalIndicesParameters == null ||
            externalIndicesParameters.pathPrefixesForGenerationSignatureIndices.none { library.canonicalPath.startsWith(it) }
        ) {
            return null
        }

        return KlibSignatureIndexComponentLayout.ExternalIndex(
            root = externalIndicesParameters.externalSignatureIndicesDir,
            libraryName = library.uniqueName,
            targetDiscriminator = externalIndicesParameters.targetDiscriminator,
            libraryFingerprintHash = library.lazyEvaluatedFingerprintHash,
        )
    }

    @OptIn(ExperimentalPathApi::class)
    private fun getExtractedSignaturesFromExternalIndex(
        externalIndexLayout: KlibSignatureIndexComponentLayout.ExternalIndex,
    ): ExtractedSignatures {
        // If there is the external index (possibly, just written by another thread or process), return signatures from it.
        KlibSignatureIndexComponent.createComponentIfDataInKlibIsAvailable(
            layoutReader = KlibLayoutReader.FromDirectory(
                klibDir = externalIndexLayout.root,
                layoutBuilder = { externalIndexLayout }
            )
        )?.let { return it.toExtractedSignatures() }

        // Else, compute signatures and store them as the external index on the file system.
        val [exported, imported] = delegate.extractOnlyTopLevelPublicSignatures()

        try {
            KlibSignatureIndexComponentWriterImpl(
                exportedTopLevelSignatures = exported,
                importedTopLevelSignatures = imported,
                layoutBuilder = { externalIndexLayout },
            ).writeTo(externalIndexLayout.root)
        } catch (e: Throwable) {
            // Don't leave a partially written index on the file system.
            runCatching { externalIndexLayout.indicesDir.deleteRecursively() }
            throw e
        }

        return ExtractedSignatures(exported, imported)
    }

    companion object {
        private fun KlibSignatureIndexComponent.toExtractedSignatures() = ExtractedSignatures(
            declaredSignatures = exportedTopLevelSignatures,
            importedSignatures = importedTopLevelSignatures,
        )
    }
}
