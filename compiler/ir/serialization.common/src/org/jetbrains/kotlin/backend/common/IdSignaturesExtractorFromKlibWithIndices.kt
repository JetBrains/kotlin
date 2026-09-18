/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.IdSignaturesExtractor.ExtractedSignatures
import org.jetbrains.kotlin.library.KotlinLibrary

class IdSignaturesExtractorFromKlibWithIndices(
    private val library: KotlinLibrary,
    private val delegate: IdSignaturesExtractor
) : IdSignaturesExtractor {
    override fun extractAllPublicSignatures() = delegate.extractAllPublicSignatures()

    override fun extractOnlyTopLevelPublicSignatures(): ExtractedSignatures {
        val component = library.getComponent(KlibSignatureIndexComponent.Kind)
            ?: return delegate.extractOnlyTopLevelPublicSignatures()

        return ExtractedSignatures(
            declaredSignatures = component.exportedTopLevelSignatures,
            importedSignatures = component.importedTopLevelSignatures,
        )
    }
}
