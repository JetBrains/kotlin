/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaKDocProvider
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.kdoc.psi.api.KDocCommentDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNonPublicApi
import org.jetbrains.kotlin.analysis.api.kdoc.findKDoc as findKDocEndpoint

@OptIn(KtNonPublicApi::class)
internal class KaKDocProviderBridge(
    override val analysisSessionProvider: () -> KaSession,
) : KaBaseSessionComponent<KaSession>(), KaKDocProvider {
    override fun KtDeclaration.findKDoc(): KDocCommentDescriptor? =
        context(analysisSession) { findKDocEndpoint() }

    override fun KaDeclarationSymbol.findKDoc(): KDocCommentDescriptor? =
        context(analysisSession) { findKDocEndpoint() }
}
