/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fe10.test

import org.jetbrains.kotlin.analysis.api.fe10.test.configurator.AnalysisApiFe10TestConfiguratorFactory
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.AnalysisApiTestGenerator
import org.jetbrains.kotlin.generators.tests.analysis.api.generateAnalysisApiTests

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        AnalysisApiTestGenerator(this, listOf(AnalysisApiFe10TestConfiguratorFactory)).run {
            generateAnalysisApiTests()
        }
    }
}