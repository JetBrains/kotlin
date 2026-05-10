/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText

import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibKlibSerializationPhase
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.invokeToplevel
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedOutputArtifact
import org.jetbrains.kotlin.test.model.BackendFacade
import org.jetbrains.kotlin.test.model.BackendKinds.IrBackend
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.ArtifactKind

@Suppress("UNCHECKED_CAST")
class SerializationCliJKlibFacade(testServices: TestServices) :
    BackendFacade<IrBackendInput, JKlibKLibWithArtifact>(testServices, IrBackend, ArtifactKinds.KLib as ArtifactKind<JKlibKLibWithArtifact>) {
    override fun transform(module: TestModule, inputArtifact: IrBackendInput): JKlibKLibWithArtifact {
        if (inputArtifact !is Fir2IrCliBasedOutputArtifact<*>) {
            error("input artifact is not a Fir2IrCliBasedOutputArtifact")
        }
        val cliArtifact = inputArtifact.cliArtifact
        require(cliArtifact is JKlibFir2IrPipelineArtifact)

        val serializationArtifact = JKlibKlibSerializationPhase.executePhase(cliArtifact)

        return JKlibKLibWithArtifact(serializationArtifact)
    }
}

