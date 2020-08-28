/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.transformers.PhasedFirFileResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache

internal class IdePhasedFirFileResolver(
    private val firFileBuilder: FirFileBuilder,
    private val cache: ModuleFileCache
) : PhasedFirFileResolver() {
    override fun resolveFile(firFile: FirFile, fromPhase: FirResolvePhase, toPhase: FirResolvePhase) {
        firFileBuilder.runResolve(firFile, cache, fromPhase, toPhase)
    }
}