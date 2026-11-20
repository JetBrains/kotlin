/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Isolates a test which `CompilerTestGroupingTestEngine` retries after it has failed in a grouped batch, whatever the
 * other isolators say. They have missed why the test cannot be grouped, and the reason may lie in how its files are
 * generated for a batch (like the package the test is moved into), so the retry must not generate them that way again.
 */
class RetriedTestIsolator(
    testServices: TestServices,
    private val isRetried: () -> Boolean,
) : GroupingTestIsolator(testServices, affectsFileGenerators = true) {
    override fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken =
        if (isRetried()) BatchToken.Isolated else BatchToken.Regular
}
