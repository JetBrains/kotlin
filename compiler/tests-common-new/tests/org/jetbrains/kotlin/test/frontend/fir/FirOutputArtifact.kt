/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.fir.AbstractFirAnalyzerFacade
import org.jetbrains.kotlin.fir.FirAnalyzerFacade
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestFile

abstract class FirOutputArtifact : ResultingArtifact.FrontendOutput<FirOutputArtifact>() {
    abstract val session: FirSession
    abstract val firAnalyzerFacade: AbstractFirAnalyzerFacade
    abstract val allFirFiles: Map<TestFile, FirFile>

    override val kind: FrontendKinds.FIR
        get() = FrontendKinds.FIR


    val firFiles: Map<TestFile, FirFile> by lazy { allFirFiles.filterKeys { !it.isAdditional } }
}

data class FirOutputArtifactImpl(
    override val session: FirSession,
    override val allFirFiles: Map<TestFile, FirFile>,
    override val firAnalyzerFacade: FirAnalyzerFacade
) : FirOutputArtifact()