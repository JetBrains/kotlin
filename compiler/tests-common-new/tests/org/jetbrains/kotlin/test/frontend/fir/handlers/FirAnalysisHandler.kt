/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.FrontendOutputHandler
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

abstract class FirAnalysisHandler(
    testServices: TestServices
) : FrontendOutputHandler<FirOutputArtifact>(
    testServices,
    FrontendKinds.FIR
) {
    protected val File.nameWithoutFirExtension: String
        get() = nameWithoutExtension.removeSuffix(".fir")
}
