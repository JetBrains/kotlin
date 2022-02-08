/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators

fun generateTestGroupSuiteWithJUnit5(
    args: Array<String>,
    additionalMethodGenerators: List<MethodGenerator<Nothing>> = emptyList(),
    init: TestGroupSuite.() -> Unit
) {
    generateTestGroupSuiteWithJUnit5(InconsistencyChecker.hasDryRunArg(args), additionalMethodGenerators, init)
}

fun generateTestGroupSuiteWithJUnit5(
    dryRun: Boolean = false,
    additionalMethodGenerators: List<MethodGenerator<Nothing>> = emptyList(),
    init: TestGroupSuite.() -> Unit
) {
    val suite = TestGroupSuite(ReflectionBasedTargetBackendComputer).apply(init)
    for (testGroup in suite.testGroups) {
        for (testClass in testGroup.testClasses) {
            val (changed, testSourceFilePath) = NewTestGeneratorImpl(additionalMethodGenerators).generateAndSave(testClass, dryRun)
            if (changed) {
                InconsistencyChecker.inconsistencyChecker(dryRun).add(testSourceFilePath)
            }
        }
    }
}
