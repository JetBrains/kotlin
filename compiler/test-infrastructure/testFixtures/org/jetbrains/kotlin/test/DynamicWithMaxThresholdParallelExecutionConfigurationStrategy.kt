/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfiguration
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfigurationStrategy

class DynamicWithMaxThresholdParallelExecutionConfigurationStrategy : ParallelExecutionConfigurationStrategy {
    companion object {
        const val FIXED_THRESHOLD_PROP = "junit.jupiter.execution.parallel.config.fixed.threshold"
        private const val DEFAULT_VALUE = 16

        // Copied from org.junit.platform.engine.support.hierarchical.DefaultParallelExecutionConfigurationStrategy
        private const val KEEP_ALIVE_SECONDS = 30

        fun computeParallelism(configurationParameters: ConfigurationParameters, vararg propertiesToCheck: String): Int {
            val threshold = propertiesToCheck.firstNotNullOfOrNull {
                configurationParameters[it].orElse(null)?.toIntOrNull()?.also {
                    require(it > 0) { "Threads threshold must be positive, but was $it" }
                }
            } ?: DEFAULT_VALUE
            val cpuCores = Runtime.getRuntime().availableProcessors()
            return minOf(cpuCores, threshold)
        }
    }

    override fun createConfiguration(configurationParameters: ConfigurationParameters): ParallelExecutionConfiguration {
        val parallelism = computeParallelism(configurationParameters, FIXED_THRESHOLD_PROP)
        return object : ParallelExecutionConfiguration {
            override fun getParallelism(): Int = parallelism
            override fun getMinimumRunnable(): Int = parallelism
            override fun getMaxPoolSize(): Int = 256 + parallelism
            override fun getCorePoolSize(): Int = parallelism
            override fun getKeepAliveSeconds(): Int = KEEP_ALIVE_SECONDS
        }
    }
}
