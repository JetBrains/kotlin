/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfiguration
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfigurationStrategy

class DynamicWithMaxThresholdParallelExecutionConfigurationStrategy : ParallelExecutionConfigurationStrategy {
    companion object {
        // Full property name is junit.jupiter.execution.parallel.config.fixed.threshold
        private const val FIXED_THRESHOLD = "fixed.threshold"
        private const val DEFAULT_VALUE = 16

        // Copied from org.junit.platform.engine.support.hierarchical.DefaultParallelExecutionConfigurationStrategy
        private const val KEEP_ALIVE_SECONDS = 30
    }

    override fun createConfiguration(configurationParameters: ConfigurationParameters): ParallelExecutionConfiguration {
        val threshold = configurationParameters[FIXED_THRESHOLD].map { it.toIntOrNull() }.orElse(null) ?: DEFAULT_VALUE
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val parallelism = if (threshold > 0) minOf(cpuCores, threshold) else cpuCores
        return object : ParallelExecutionConfiguration {
            override fun getParallelism(): Int = parallelism
            override fun getMinimumRunnable(): Int = parallelism
            override fun getMaxPoolSize(): Int = 256 + parallelism
            override fun getCorePoolSize(): Int = parallelism
            override fun getKeepAliveSeconds(): Int = KEEP_ALIVE_SECONDS
        }
    }
}
