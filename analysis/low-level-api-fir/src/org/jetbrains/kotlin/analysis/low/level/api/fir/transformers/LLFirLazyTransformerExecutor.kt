/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector

internal class LLFirLazyTransformerExecutor {
    companion object {
        fun execute(
            phase: FirResolvePhase,
            designation: FirDesignationWithFile,
            scopeSession: ScopeSession,
            phaseRunner: LLFirPhaseRunner,
            lockProvider: LLFirLockProvider,
            towerDataContextCollector: FirTowerDataContextCollector?
        ) {

            val lazyTransformer = LazyTransformerFactory.createLazyTransformer(
                phase,
                designation,
                scopeSession,
                lockProvider,
                towerDataContextCollector
            )
            lazyTransformer.transformDeclaration(phaseRunner)
        }
    }
}