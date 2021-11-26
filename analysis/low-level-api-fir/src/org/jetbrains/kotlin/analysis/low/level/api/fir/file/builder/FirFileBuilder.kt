/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder

import org.jetbrains.kotlin.analysis.api.impl.barebone.annotations.ThreadSafe
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.PsiHandlingMode
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.FirPhaseRunner
import org.jetbrains.kotlin.psi.KtFile

/**
 * Responsible for building [FirFile] by [KtFile]
 * Stateless, all caches are stored in [ModuleFileCache] passed into corresponding functions
 */
@ThreadSafe
internal class FirFileBuilder(
    private val scopeProvider: FirScopeProvider,
    val firPhaseRunner: FirPhaseRunner
) {
    /**
     * Builds a [FirFile] by given [ktFile] and records it's parenting info if it not present in [cache]
     * [FirFile] building a happens at most once per each [KtFile]
     */
    fun buildRawFirFileWithCaching(
        ktFile: KtFile,
        cache: ModuleFileCache,
        preferLazyBodies: Boolean
    ): FirFile = cache.fileCached(ktFile) {
        RawFirBuilder(
            cache.session,
            scopeProvider,
            psiMode = PsiHandlingMode.IDE,
            bodyBuildingMode = BodyBuildingMode.lazyBodies(preferLazyBodies)
        ).buildFirFile(ktFile)
    }
}


