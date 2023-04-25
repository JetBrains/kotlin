/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.AbstractFirAnalyzerFacade
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule

// Only MPP contains several parts inside FirOutputArtifact, other projects only contain single part.
data class FirOutputPartForDependsOnModule(
    val module: TestModule,
    val session: FirSession,
    val firAnalyzerFacade: AbstractFirAnalyzerFacade,
    val firFiles: Map<TestFile, FirFile>
)

abstract class FirOutputArtifact(val partsForDependsOnModules: List<FirOutputPartForDependsOnModule>) : ResultingArtifact.FrontendOutput<FirOutputArtifact>() {
    val allFirFiles: Map<TestFile, FirFile> = partsForDependsOnModules.fold(emptyMap()) { acc, part -> acc + part.firFiles }

    override val kind: FrontendKinds.FIR
        get() = FrontendKinds.FIR

    val mainFirFiles: Map<TestFile, FirFile> by lazy { allFirFiles.filterKeys { !it.isAdditional } }

    val hasErrors: Boolean by lazy {
        partsForDependsOnModules.any { part ->
            part.firAnalyzerFacade.runCheckers().values.any { diagnostics -> diagnostics.any { it.severity == Severity.ERROR } }
        }
    }
}

class FirOutputArtifactImpl(parts: List<FirOutputPartForDependsOnModule>) : FirOutputArtifact(parts)