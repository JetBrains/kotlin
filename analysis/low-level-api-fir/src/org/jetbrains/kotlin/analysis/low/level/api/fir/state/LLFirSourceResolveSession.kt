/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.analysis.project.structure.KtBuiltinsModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError

internal class LLFirSourceResolveSession(
    moduleProvider: LLModuleProvider,
    sessionProvider: LLSessionProvider
) : LLFirResolvableResolveSession(
    moduleProvider = moduleProvider,
    moduleKindProvider = LLSourceModuleKindProvider,
    sessionProvider = sessionProvider,
    diagnosticProvider = LLSourceDiagnosticProvider(moduleProvider, sessionProvider)
)

private object LLSourceModuleKindProvider : LLModuleKindProvider {
    override fun getKind(module: KtModule): KtModuleKind {
        return when (module) {
            is KtSourceModule -> KtModuleKind.RESOLVABLE_MODULE
            is KtBuiltinsModule,
            is KtLibraryModule -> KtModuleKind.BINARY_MODULE
            else -> unexpectedElementError("module", module)
        }
    }
}