/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.impl.testConfiguration
import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.model.SimpleTestFailureSuppressor
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

class MutedTestsIsolator(testServices: TestServices) : GroupingTestIsolator(testServices, affectsFileGenerators = false) {
    override fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken {
        @OptIn(TestInfrastructureInternals::class)
        val testIsMuted = testServices.testConfiguration.failureSuppressors.filterIsInstance<SimpleTestFailureSuppressor>().any { it.testIsMuted() }
        if (testIsMuted) return BatchToken.Isolated
        return BatchToken.Regular
    }
}
