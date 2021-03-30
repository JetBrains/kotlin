/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.FrontendOutputHandler
import org.jetbrains.kotlin.test.services.TestServices

abstract class ClassicFrontendAnalysisHandler(
    testServices: TestServices,
    failureDisablesNextSteps: Boolean = false,
    doNotRunIfThereWerePreviousFailures: Boolean = false
) : FrontendOutputHandler<ClassicFrontendOutputArtifact>(
    testServices,
    FrontendKinds.ClassicFrontend,
    failureDisablesNextSteps,
    doNotRunIfThereWerePreviousFailures
)


