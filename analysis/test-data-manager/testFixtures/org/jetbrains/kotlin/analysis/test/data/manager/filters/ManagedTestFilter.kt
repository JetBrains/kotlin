/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.filters

import org.junit.platform.engine.FilterResult
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.PostDiscoveryFilter

/**
 * Include only tests that implement ManagedTest.
 *
 * This filter checks both the test class and its enclosing classes,
 * so nested test classes within a ManagedTest are also included.
 *
 * @param goldenOnly When true, only includes tests with an empty variant chain (golden tests).
 */
internal class ManagedTestFilter(private val goldenOnly: Boolean = false) : PostDiscoveryFilter {
    override fun apply(descriptor: TestDescriptor): FilterResult {
        val source = descriptor.source.orElse(null) ?: return FilterResult.excluded("No source")
        val testClass = when (source) {
            is ClassSource -> source.javaClass
            is MethodSource -> source.javaClass
            else -> return FilterResult.excluded("Unsupported source type")
        }

        val managedTestClass = testClass.findManagedTestClass()
            ?: return FilterResult.excluded("Does not implement ManagedTest")

        // Filter out non-golden classes (classes with non-empty variant chain)
        // variantChain is a class-level property, so filtering at class level is efficient
        if (goldenOnly) {
            val variantChain = managedTestClass.getVariantChain()
            if (variantChain.isNotEmpty()) {
                return FilterResult.excluded("Non-golden class: $variantChain")
            }
        }

        return FilterResult.included("Implements ManagedTest" + if (goldenOnly) " (golden)" else "")
    }
}
