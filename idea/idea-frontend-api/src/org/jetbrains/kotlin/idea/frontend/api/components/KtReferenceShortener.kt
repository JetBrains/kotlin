/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.psi.KtFile

abstract class KtReferenceShortener : KtAnalysisSessionComponent() {
    abstract fun collectShortenings(file: KtFile, selection: TextRange): ShortenCommand
}

interface KtReferenceShortenerMixIn : KtAnalysisSessionMixIn {
    fun collectPossibleReferenceShortenings(file: KtFile, selection: TextRange): ShortenCommand =
        analysisSession.referenceShortener.collectShortenings(file, selection)
}

interface ShortenCommand {
    fun invokeShortening()
}