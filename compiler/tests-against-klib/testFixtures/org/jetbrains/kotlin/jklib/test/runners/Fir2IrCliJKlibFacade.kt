package org.jetbrains.kotlin.jklib.test.runners

import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibFir2IrPipelinePhase
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibFrontendPipelineArtifact
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliFacade
import org.jetbrains.kotlin.test.services.TestServices

class Fir2IrCliJKlibFacade(
    testServices: TestServices
) : Fir2IrCliFacade<JKlibFir2IrPipelinePhase, JKlibFrontendPipelineArtifact, JKlibFir2IrPipelineArtifact>(testServices, JKlibFir2IrPipelinePhase)
