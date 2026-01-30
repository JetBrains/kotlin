/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.phases

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.FrontendFilesForPluginsGenerationPipelinePhase
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.native.FirOutput
import org.jetbrains.kotlin.native.firFrontendWithLightTree
import org.jetbrains.kotlin.native.firFrontendWithPsi

internal fun PhaseContext.firFrontend(input: KotlinCoreEnvironment): FirOutput {
    var output = if (input.configuration.getBoolean(CommonConfigurationKeys.USE_LIGHT_TREE)) {
        firFrontendWithLightTree(input)
    } else {
        firFrontendWithPsi(input)
    }
    if (output is FirOutput.Full) {
        output = FirOutput.Full(FrontendFilesForPluginsGenerationPipelinePhase.createFilesWithGeneratedDeclarations(output.firResult))
    }
    return output
}

private val FIRPhase = createSimpleNamedCompilerPhase(
        "FirFrontend",
        outputIfNotEnabled = { _, _, _, _ -> FirOutput.ShouldNotGenerateCode }
) { context: PhaseContext, input: KotlinCoreEnvironment -> context.firFrontend(input) }

public fun <T : PhaseContext> PhaseEngine<T>.runFirFrontend(environment: KotlinCoreEnvironment): FirOutput {
    val languageVersion = environment.configuration.languageVersionSettings.languageVersion
    val kotlinSourceRoots = environment.configuration.kotlinSourceRoots
    if (!languageVersion.usesK2 && kotlinSourceRoots.isNotEmpty()) {
        throw Error("Attempt to run K2 from unsupported LV=${languageVersion}")
    }

    return this.runPhase(FIRPhase, environment)
}
