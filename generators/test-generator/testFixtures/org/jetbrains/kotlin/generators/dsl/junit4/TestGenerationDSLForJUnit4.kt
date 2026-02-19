/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.dsl.junit4

import org.jetbrains.kotlin.generators.InconsistencyChecker
import org.jetbrains.kotlin.generators.InconsistencyChecker.Companion.inconsistencyChecker
import org.jetbrains.kotlin.generators.allowGenerationOnTeamCity
import org.jetbrains.kotlin.generators.dsl.TestGroupSuite
import org.jetbrains.kotlin.generators.dsl.forEachTestClassParallel
import org.jetbrains.kotlin.generators.model.TestInfraRevision
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

fun generateTestGroupSuiteWithJUnit4(
    args: Array<String>,
    mainClassName: String? = TestGeneratorUtil.getMainClassName(),
    init: TestGroupSuite.() -> Unit
) {
    generateTestGroupSuiteWithJUnit4(
        dryRun = InconsistencyChecker.hasDryRunArg(args),
        allowGenerationOnTeamCity = args.allowGenerationOnTeamCity(),
        mainClassName,
        init
    )
}

fun generateTestGroupSuiteWithJUnit4(
    dryRun: Boolean = false,
    allowGenerationOnTeamCity: Boolean = false,
    mainClassName: String? = TestGeneratorUtil.getMainClassName(),
    init: TestGroupSuite.() -> Unit,
) {
    val suite = TestGroupSuite(TestInfraRevision.LegacyJUnit4, defaultSkipTestAllFilesCheck = false).apply(init)
    suite.forEachTestClassParallel { testClass ->
        val (changed, testSourceFilePath) = TestGeneratorForJUnit4
            .generateAndSave(testClass, dryRun, allowGenerationOnTeamCity, mainClassName)
        if (changed) {
            inconsistencyChecker(dryRun).add(testSourceFilePath)
        }
    }
}
