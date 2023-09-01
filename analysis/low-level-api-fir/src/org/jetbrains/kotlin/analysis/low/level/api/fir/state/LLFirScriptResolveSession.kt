/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError

internal class LLFirScriptResolveSession(
    moduleProvider: LLModuleProvider,
    sessionProvider: LLSessionProvider
) : LLFirResolvableResolveSession(
    moduleProvider = moduleProvider,
    moduleKindProvider = LLScriptModuleKindProvider(moduleProvider.useSiteModule),
    sessionProvider = sessionProvider
) {
    override val diagnosticProvider = LLSourceDiagnosticProvider(moduleProvider, sessionProvider)
}

private class LLScriptModuleKindProvider(private val useSiteModule: KtModule) : LLModuleKindProvider {
    override fun getKind(module: KtModule): KtModuleKind {
        return when (module) {
            useSiteModule, is KtSourceModule -> KtModuleKind.RESOLVABLE_MODULE
            is KtBuiltinsModule, is KtLibraryModule -> KtModuleKind.BINARY_MODULE
            else -> unexpectedElementError("module", module)
        }
    }
}