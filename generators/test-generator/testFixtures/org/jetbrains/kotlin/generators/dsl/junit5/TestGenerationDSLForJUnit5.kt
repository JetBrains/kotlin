/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.dsl.junit5

import org.jetbrains.kotlin.generators.InconsistencyChecker
import org.jetbrains.kotlin.generators.allowGenerationOnTeamCity
import org.jetbrains.kotlin.generators.dsl.TestGroupSuite
import org.jetbrains.kotlin.generators.dsl.forEachTestClassParallel
import org.jetbrains.kotlin.generators.model.TestInfraRevision
import org.jetbrains.kotlin.generators.skipTestAllFilesCheck
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun generateTestGroupSuiteWithJUnit5(
    args: Array<String>,
    // Main class name can only be determined when running on the main thread.
    // If this call is not made on the main thread, the main class name must be injected.
    mainClassName: String? = TestGeneratorUtil.getMainClassName(),
    init: TestGroupSuite.() -> Unit,
) {
    generateTestGroupSuiteWithJUnit5(
        dryRun = InconsistencyChecker.hasDryRunArg(args),
        allowGenerationOnTeamCity = args.allowGenerationOnTeamCity(),
        skipTestAllFilesCheck = args.skipTestAllFilesCheck(),
        mainClassName,
        init
    )
}

fun generateTestGroupSuiteWithJUnit5(
    dryRun: Boolean = false,
    allowGenerationOnTeamCity: Boolean = false,
    skipTestAllFilesCheck: Boolean = false,
    // See above
    mainClassName: String? = TestGeneratorUtil.getMainClassName(),
    init: TestGroupSuite.() -> Unit,
) {
    val suite = TestGroupSuite(TestInfraRevision.StandardJUnit5, skipTestAllFilesCheck).apply(init)
    suite.forEachTestClassParallel { testClass ->
        val (changed, testSourceFilePath) = TestGeneratorForJUnit5
            .generateAndSave(testClass, dryRun, allowGenerationOnTeamCity, mainClassName)
        if (changed) {
            InconsistencyChecker.inconsistencyChecker(dryRun).add(testSourceFilePath)
        }
    }
}
