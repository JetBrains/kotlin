/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.js

import org.jetbrains.kotlin.cli.pipeline.web.WebIncrementalCachePreparationPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.JsICContext
import org.jetbrains.kotlin.ir.backend.js.ic.JsModuleArtifact
import org.jetbrains.kotlin.js.config.WebArtifactConfiguration

object JsIncrementalCachePreparationPipelinePhase : WebIncrementalCachePreparationPipelinePhase<JsModuleArtifact, JsICContext>(
    name = JsIncrementalCachePreparationPipelinePhase::class.java.simpleName,
) {
    override fun createIcContext(configuration: CompilerConfiguration, artifactConfiguration: WebArtifactConfiguration): JsICContext =
        JsICContext(artifactConfiguration.granularity)
}
