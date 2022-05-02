/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder

import org.jetbrains.kotlin.analysis.api.impl.barebone.annotations.ThreadSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.PsiHandlingMode
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.psi.KtFile

/**
 * Responsible for building [FirFile] by [KtFile]
 */
@ThreadSafe
internal class LLFirFileBuilder(
    val moduleComponents: LLFirModuleResolveComponents,
) {
    fun buildRawFirFileWithCaching(ktFile: KtFile): FirFile = moduleComponents.cache.fileCached(ktFile) {
        RawFirBuilder(
            moduleComponents.session,
            moduleComponents.scopeProvider,
            psiMode = PsiHandlingMode.IDE,
            bodyBuildingMode = BodyBuildingMode.LAZY_BODIES
        ).buildFirFile(ktFile)
    }
}


