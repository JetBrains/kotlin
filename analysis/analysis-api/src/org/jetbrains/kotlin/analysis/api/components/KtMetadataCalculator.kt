/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

public abstract class KtMetadataCalculator : KtAnalysisSessionComponent() {
    public abstract fun calculate(ktClass: KtClassOrObject): Metadata?
    public abstract fun calculate(ktFile: KtFile): Metadata?
    public abstract fun calculate(ktFiles: Iterable<KtFile>): Metadata?
}

public interface KtMetadataCalculatorMixIn : KtAnalysisSessionMixIn {
    /**
     * Returns [Metadata] annotation for the provided [KtClassOrObject] or null if this functionality is not supported
     */
    public fun KtClassOrObject.calculateMetadata(): Metadata? =
        withValidityAssertion { analysisSession.ktMetadataCalculator.calculate(this) }

    /**
     * Returns [Metadata] annotation for the provided [KtFile] representing a single-file or null if this functionality is not supported
     */
    public fun KtFile.calculateMetadata(): Metadata? =
        withValidityAssertion { analysisSession.ktMetadataCalculator.calculate(this) }

    /**
     * Returns [Metadata] annotation for the provided files representing a multi-file facade or null if this functionality is not supported
     */
    public fun Iterable<KtFile>.calculateMetadata(): Metadata? =
        withValidityAssertion { analysisSession.ktMetadataCalculator.calculate(this) }
}
