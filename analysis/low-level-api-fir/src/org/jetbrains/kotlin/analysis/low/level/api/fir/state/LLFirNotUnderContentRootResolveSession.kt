/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.project.structure.KtModule

internal class LLFirNotUnderContentRootResolveSession(
    useSiteKtModule: KtModule,
    useSiteSessionFactory: (KtModule) -> LLFirSession
) : LLFirResolvableResolveSession(
    useSiteKtModule = useSiteKtModule,
    moduleKindProvider = LLNotUnderContentRootModuleKindProvider(useSiteKtModule),
    useSiteSessionFactory = useSiteSessionFactory
) {
    override val diagnosticProvider: LLDiagnosticProvider
        get() = LLEmptyDiagnosticProvider
}

private class LLNotUnderContentRootModuleKindProvider(private val useSiteModule: KtModule) : LLModuleKindProvider {
    override fun getKind(module: KtModule): KtModuleKind {
        return when (module) {
            useSiteModule -> KtModuleKind.RESOLVABLE_MODULE
            else -> KtModuleKind.BINARY_MODULE
        }
    }
}