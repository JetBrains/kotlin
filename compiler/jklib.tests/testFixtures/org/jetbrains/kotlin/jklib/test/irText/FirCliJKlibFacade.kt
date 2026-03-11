/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText

import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibFrontendPipelinePhase
import org.jetbrains.kotlin.test.frontend.fir.FirCliFacade
import org.jetbrains.kotlin.test.services.TestServices

class FirCliJKlibFacade(
    testServices: TestServices,
) : FirCliFacade<JKlibFrontendPipelinePhase, JKlibFrontendPipelineArtifact>(testServices, JKlibFrontendPipelinePhase)
