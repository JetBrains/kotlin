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
 */
internal object ManagedTestFilter : PostDiscoveryFilter {
    override fun apply(descriptor: TestDescriptor): FilterResult {
        val source = descriptor.source.orElse(null) ?: return FilterResult.excluded("No source")
        val testClass = when (source) {
            is ClassSource -> source.javaClass
            is MethodSource -> source.javaClass
            else -> return FilterResult.excluded("Unsupported source type")
        }

        return if (testClass.findManagedTestClass() != null) {
            FilterResult.included("Implements ManagedTest")
        } else {
            FilterResult.excluded("Does not implement ManagedTest")
        }
    }
}