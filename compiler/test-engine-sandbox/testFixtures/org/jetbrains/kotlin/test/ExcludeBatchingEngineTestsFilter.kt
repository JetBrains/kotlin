/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor
import org.junit.jupiter.engine.descriptor.MethodBasedTestDescriptor
import org.junit.platform.engine.FilterResult
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.launcher.PostDiscoveryFilter
import kotlin.jvm.optionals.getOrNull

/**
 * Filters out tests inheriting [AbstractTwoStageKotlinCompilerTest] from engines other than
 * [CompilerTestGroupingTestEngine].
 *
 * This prevents the standard JUnit Jupiter engine from running tests that should
 * only be executed by the batching engine.
 */
class ExcludeBatchingEngineTestsFilter : PostDiscoveryFilter {
    override fun apply(descriptor: TestDescriptor): FilterResult {
        // Allow all tests from the batching engine
        if (descriptor.uniqueId.engineId.getOrNull() == CompilerTestGroupingTestEngine.ID) {
            return FilterResult.included("Batching engine test")
        }

        // For other engines (e.g., junit-jupiter), exclude tests with @UseBatchingEngine
        if (shouldExclude(descriptor)) {
            return FilterResult.excluded("Test class uses @UseBatchingEngine annotation")
        }

        return FilterResult.included("Not a batching engine test")
    }

    private fun shouldExclude(descriptor: TestDescriptor): Boolean {
        // Check if this descriptor or any parent has @UseBatchingEngine
        var current: TestDescriptor? = descriptor
        while (current != null) {
            val testClass = when (current) {
                is MethodBasedTestDescriptor -> current.testClass
                is ClassBasedTestDescriptor -> current.testClass
                else -> null
            }
            if (testClass != null && testClass.isTwoStageKotlinCompilerTest()) {
                return true
            }
            current = current.parent.getOrNull()
        }
        return false
    }
}

internal fun Class<*>.isTwoStageKotlinCompilerTest(): Boolean {
    val targetClass = AbstractTwoStageKotlinCompilerTest::class.java
    var currentClass: Class<*>? = this
    while (currentClass != null) {
        if (currentClass == targetClass) {
            return true
        }
        currentClass = currentClass.superclass
    }
    return false
}
