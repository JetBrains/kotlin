/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.compilerPlugins

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope

internal class KaFirCompilerPluginGeneratedDeclarations(
    private val backingTopLevelDeclarationsScope: KaScope,
) : org.jetbrains.kotlin.analysis.api.components.KaCompilerPluginGeneratedDeclarations {
    override val token: KaLifetimeToken = backingTopLevelDeclarationsScope.token

    override val topLevelDeclarationsScope: KaScope
        get() = withValidityAssertion { backingTopLevelDeclarationsScope }
}
