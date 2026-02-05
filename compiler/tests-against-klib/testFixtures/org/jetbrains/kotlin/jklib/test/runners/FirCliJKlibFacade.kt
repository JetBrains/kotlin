package org.jetbrains.kotlin.jklib.test.runners

import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibConfigurationPhase
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibFrontendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.test.frontend.fir.FirCliFacade
import org.jetbrains.kotlin.test.services.TestServices

class FirCliJKlibFacade(
    testServices: TestServices,
) : FirCliFacade<JKlibFrontendPipelinePhase, JKlibFrontendPipelineArtifact>(testServices, JKlibFrontendPipelinePhase)
