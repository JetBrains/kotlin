/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api.dsl

import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5

class AnalysisApiTestGenerator(val suite: TestGroupSuite) {
    fun group(init: AnalysisApiTestGroup.() -> Unit) {
        AnalysisApiTestGroup(this, { true }, null).init()
    }
}

fun generate(args: Array<String>, init: AnalysisApiTestGroup.() -> Unit) {
    generateTestGroupSuiteWithJUnit5(args, additionalMethodGenerators = listOf(FrontendConfiguratorTestGenerator)) {
        AnalysisApiTestGenerator(this).group(init)
    }
}