/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirReplSnippetConfiguratorExtensionImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.isReplSnippetSource
import kotlin.script.experimental.api.repl
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.with

/**
 * The class is a copy of [org.jetbrains.kotlin.scripting.compiler.plugin.FirReplCompilerExtensionRegistrar]
 * adapted for the usage outside the compiler context.
 * The difference is that it omits redundant extensions and adjusts required options.
 */
internal class FirReplCompilerExtensionIdeRegistrar(
    private val hostConfiguration: ScriptingHostConfiguration,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        val replHostConfiguration = hostConfiguration.with {
            repl {
                isReplSnippetSource { _, sourceElement ->
                    @OptIn(KtExperimentalApi::class)
                    sourceElement is KtRealPsiSourceElement && (sourceElement.psi as? KtScript)?.isReplSnippet == true
                }
            }
        }

        +FirReplSnippetConfiguratorExtensionImpl.getFactory(replHostConfiguration)
    }
}
