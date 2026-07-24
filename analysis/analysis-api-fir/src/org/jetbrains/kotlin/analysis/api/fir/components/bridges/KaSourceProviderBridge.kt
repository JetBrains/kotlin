/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSourceProvider
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.klibSourceFileName as klibSourceFileNameEndpoint

/**
 * Routes the legacy [KaSourceProvider] surface through the new public `context(session: KaSession)` source endpoints, which in turn
 * reach the [org.jetbrains.kotlin.analysis.api.internals.KaInternalsSourceProvider] proxy.
 */
internal class KaSourceProviderBridge(
    override val analysisSessionProvider: () -> KaSession,
) : KaBaseSessionComponent<KaSession>(), KaSourceProvider {
    override val KaDeclarationSymbol.klibSourceFileName: String?
        get() = context(analysisSession) { klibSourceFileNameEndpoint }
}
