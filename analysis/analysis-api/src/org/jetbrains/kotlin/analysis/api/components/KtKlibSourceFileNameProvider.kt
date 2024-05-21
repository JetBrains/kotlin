/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.psi.KtDeclaration

@KaAnalysisNonPublicApi
public abstract class KaKlibSourceFileNameProvider : KaSessionComponent() {
    public abstract fun getKlibSourceFileName(declaration: KaDeclarationSymbol): String?
}

@KaAnalysisNonPublicApi
public typealias KtKlibSourceFileNameProvider = KaKlibSourceFileNameProvider

@KaAnalysisNonPublicApi
public interface KaKlibSourceFileProviderMixIn : KaSessionMixIn {
    /**
     * If [KtDeclaration] is a deserialized, klib based symbol, then information about the original
     * [SourceFile] might be retained.
     */
    public fun KaDeclarationSymbol.getKlibSourceFileName(): String? =
        withValidityAssertion { analysisSession.klibSourceFileProvider.getKlibSourceFileName(this) }
}

@KaAnalysisNonPublicApi
public typealias KtKlibSourceFileProviderMixIn = KaKlibSourceFileProviderMixIn