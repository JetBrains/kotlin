/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.impl

import org.jetbrains.kotlin.generators.InconsistencyChecker
import org.jetbrains.kotlin.generators.InconsistencyChecker.Companion.inconsistencyChecker
import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.forEachTestClassParallel
import org.jetbrains.kotlin.generators.testGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun generateTestGroupSuite(
    args: Array<String>,
    mainClassName: String? = TestGeneratorUtil.getMainClassName(),
    init: TestGroupSuite.() -> Unit
) {
    generateTestGroupSuite(InconsistencyChecker.hasDryRunArg(args), mainClassName, init)
}

fun generateTestGroupSuite(
    dryRun: Boolean = false,
    mainClassName: String? = TestGeneratorUtil.getMainClassName(),
    init: TestGroupSuite.() -> Unit,
) {
    val suite = testGroupSuite(init)
    suite.forEachTestClassParallel { testClass ->
        val (changed, testSourceFilePath) = TestGeneratorImpl.generateAndSave(testClass, dryRun, mainClassName)
        if (changed) {
            inconsistencyChecker(dryRun).add(testSourceFilePath)
        }
    }
}
