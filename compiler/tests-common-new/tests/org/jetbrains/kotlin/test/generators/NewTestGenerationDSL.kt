/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.generators

import org.jetbrains.kotlin.generators.InconsistencyChecker
import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.testGroupSuite

fun generateTestGroupSuiteWithJUnit5(
    args: Array<String>,
    init: TestGroupSuite.() -> Unit
) {
    generateTestGroupSuiteWithJUnit5(InconsistencyChecker.hasDryRunArg(args), init)
}

fun generateTestGroupSuiteWithJUnit5(
    dryRun: Boolean = false,
    init: TestGroupSuite.() -> Unit
) {
    val suite = testGroupSuite(init)
    for (testGroup in suite.testGroups) {
        for (testClass in testGroup.testClasses) {
            val (changed, testSourceFilePath) = NewTestGeneratorImpl.generateAndSave(testClass, dryRun)
            if (changed) {
                InconsistencyChecker.inconsistencyChecker(dryRun).add(testSourceFilePath)
            }
        }
    }
}
