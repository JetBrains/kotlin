/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.junit.jupiter.engine.config.CachingJupiterConfiguration
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration
import org.junit.jupiter.engine.config.JupiterConfiguration
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor
import org.junit.jupiter.engine.discovery.DiscoverySelectorResolver
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine

class CompilerSecondStageBatchingTestEngine : HierarchicalTestEngine<JupiterEngineExecutionContext>() {
    override fun getId(): String = "kotlin-compiler-second-stage-batching"

    override fun createExecutionContext(request: ExecutionRequest): JupiterEngineExecutionContext {
        return JupiterEngineExecutionContext(request.engineExecutionListener, getJupiterConfiguration(request))
    }

    override fun discover(
        discoveryRequest: EngineDiscoveryRequest,
        uniqueId: UniqueId,
    ): TestDescriptor {
        val configuration = CachingJupiterConfiguration(
            DefaultJupiterConfiguration(
                discoveryRequest.configurationParameters,
                discoveryRequest.outputDirectoryProvider
            )
        )
        val engineDescriptor = JupiterEngineDescriptor(uniqueId, configuration)
        DiscoverySelectorResolver().resolveSelectors(discoveryRequest, engineDescriptor)
        return engineDescriptor
    }

    private fun getJupiterConfiguration(request: ExecutionRequest): JupiterConfiguration {
        val engineDescriptor = request.getRootTestDescriptor() as JupiterEngineDescriptor
        return engineDescriptor.configuration
    }
}
