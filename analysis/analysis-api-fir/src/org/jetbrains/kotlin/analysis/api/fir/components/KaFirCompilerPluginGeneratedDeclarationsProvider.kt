/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaCompilerPluginGeneratedDeclarationsProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KaBaseEmptyScope
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.scopes.KaScope

internal class KaFirCompilerPluginGeneratedDeclarationsProvider(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaSessionComponent<KaFirSession>(), KaCompilerPluginGeneratedDeclarationsProvider {
    override val KaModule.topLevelCompilerPluginGeneratedDeclarationsScope: KaScope
        get() = withValidityAssertion { KaBaseEmptyScope(token) }
}