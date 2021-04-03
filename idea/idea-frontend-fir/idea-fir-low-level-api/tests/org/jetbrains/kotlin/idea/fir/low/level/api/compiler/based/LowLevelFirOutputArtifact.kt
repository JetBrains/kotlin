/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.TestFile

class LowLevelFirOutputArtifact(
    override val session: FirSession,
    override val firAnalyzerFacade: LowLevelFirAnalyzerFacade,
) : FirOutputArtifact() {
    override val allFirFiles: Map<TestFile, FirFile>
        get() = firAnalyzerFacade.allFirFiles
}
