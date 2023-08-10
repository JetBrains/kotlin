/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.FrontendOutputHandler
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultDirectives
import java.io.File

abstract class FirAnalysisHandler(
    testServices: TestServices
) : FrontendOutputHandler<FirOutputArtifact>(
    testServices,
    FrontendKinds.FIR
) {
    override val doNotRunIfThereWerePreviousFailures: Boolean
        get() = DiagnosticsDirectives.STOP_ON_FAILURE in testServices.defaultDirectives
    override val failureDisablesNextSteps: Boolean
        get() = DiagnosticsDirectives.STOP_ON_FAILURE in testServices.defaultDirectives
    protected val File.nameWithoutFirExtension: String
        get() = nameWithoutExtension.removeSuffix(".fir")
}
