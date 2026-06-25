/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.components.KaCompilerPluginGeneratedDeclarations
import org.jetbrains.kotlin.analysis.api.components.KaCompilerPluginGeneratedDeclarationsProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.compilerPlugins.compilerPluginGeneratedDeclarations as compilerPluginGeneratedDeclarationsEndpoint

/**
 * Routes the legacy [KaCompilerPluginGeneratedDeclarationsProvider] surface through the new public `context(session: KaSession)`
 * `org.jetbrains.kotlin.analysis.api.compilation` endpoint, which in turn reaches the
 * [org.jetbrains.kotlin.analysis.api.internals.KaInternalsCompilerPluginGeneratedDeclarationsProvider] proxy.
 */
internal class KaCompilerPluginGeneratedDeclarationsProviderBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaCompilerPluginGeneratedDeclarationsProvider {
    override val KaModule.compilerPluginGeneratedDeclarations: KaCompilerPluginGeneratedDeclarations
        get() = context(analysisSession) {
            // The endpoint now returns the new compilation.KaCompilerPluginGeneratedDeclarations. Its only implementation also implements
            // the legacy components.KaCompilerPluginGeneratedDeclarations, so narrowing the result back to the legacy surface is safe.
            compilerPluginGeneratedDeclarationsEndpoint as KaCompilerPluginGeneratedDeclarations
        }
}
