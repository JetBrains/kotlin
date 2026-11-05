/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Isolates every test if the [ALL_TESTS_ISOLATED_PROP] system property is `true`. It is not a JUnit configuration parameter,
 * as a test configuration has no access to those.
 */
class AllTestsIsolator(testServices: TestServices) : GroupingTestIsolator(testServices, affectsFileGenerators = true) {
    override fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken =
        if (allTestsIsolated) BatchToken.Isolated else BatchToken.Regular

    companion object {
        const val ALL_TESTS_ISOLATED_PROP: String = "kotlin.test.grouping.engine.all.tests.isolated"

        private val allTestsIsolated: Boolean = java.lang.Boolean.getBoolean(ALL_TESTS_ISOLATED_PROP)
    }
}
