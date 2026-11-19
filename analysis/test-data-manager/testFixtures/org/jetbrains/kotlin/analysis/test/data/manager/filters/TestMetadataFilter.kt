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
 * A path matches whole name segments only:
 * - Directory path (`dir` or `dir/`): includes all tests whose testDataPath is inside the directory
 * - File path (`dir/file.kt`, or `dir/file` without the extension): includes the tests of that file
 *
 * A prefix that cuts a name in the middle (`dir/fi`) matches nothing.
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

    private fun matchesPath(testMetadataPath: String): Boolean = includePaths.any { includePath ->
        testMetadataPath.startsWith(includePath) && endsAtNameBoundary(testMetadataPath, includePath)
    }

    /** Whether [prefix] of [path] is followed by `/` (a directory), `.` (an extension) or nothing. */
    private fun endsAtNameBoundary(path: String, prefix: String): Boolean {
        if (prefix.isEmpty() || prefix.endsWith('/') || path.length == prefix.length) return true
        val next = path[prefix.length]
        return next == '/' || next == '.'
    }
}
