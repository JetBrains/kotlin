/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.filters

import org.junit.platform.engine.FilterResult
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.launcher.PostDiscoveryFilter

/**
 * JUnit Platform filter that includes only tests whose @TestMetadata annotation
 * matches the specified path.
 *
 * Supports:
 * - Directory path: includes all tests whose testDataPath starts with or contains the path
 * - File path: includes tests whose testDataPath matches the exact file or its directory
 */
internal class TestMetadataFilter(private val includePaths: Collection<String>) : PostDiscoveryFilter {
    override fun apply(descriptor: TestDescriptor): FilterResult {
        // Only filter test methods, not containers
        if (descriptor.type != TestDescriptor.Type.TEST) {
            return FilterResult.included("Container - check children")
        }

        val source = descriptor.source.orElse(null) ?: return FilterResult.excluded("No source")
        val testMetadataPath = source.testDataPath ?: return FilterResult.excluded("No TestMetadata annotation")
        return if (matchesPath(testMetadataPath)) {
            FilterResult.included("Matches includePaths")
        } else {
            FilterResult.excluded("Does not match includePaths")
        }
    }

    private fun matchesPath(testMetadataPath: String): Boolean = includePaths.any(testMetadataPath::startsWith)
}
