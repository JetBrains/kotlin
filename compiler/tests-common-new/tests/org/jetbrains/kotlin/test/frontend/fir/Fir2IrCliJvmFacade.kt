/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelineArtifact
import org.jetbrains.kotlin.test.services.TestServices

class Fir2IrCliJvmFacade(
    testServices: TestServices
) : Fir2IrCliFacade<JvmFir2IrPipelinePhase, JvmFrontendPipelineArtifact, JvmFir2IrPipelineArtifact>(testServices, JvmFir2IrPipelinePhase)
