/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtMetadataCalculator
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

internal class KtFe10MetadataCalculator(override val analysisSession: KtFe10AnalysisSession) : KtMetadataCalculator() {
    override fun calculate(ktClass: KtClassOrObject): Metadata? = null

    override fun calculate(ktFile: KtFile): Metadata? = null

    override fun calculate(ktFiles: Iterable<KtFile>): Metadata? = null
}