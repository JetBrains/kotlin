/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api.dsl

import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.getDefaultSuiteTestClassName
import kotlin.reflect.KClass

internal class FirAndFe10TestGroup(
    val suite: TestGroupSuite,
    val directory: String?
)

internal fun FirAndFe10TestGroup.test(
    baseClass: KClass<*>,
    generateFe10: Boolean = true,
    init: TestGroup.TestClass.() -> Unit,
) {
    analysisApiTest(
        "analysis/analysis-api-fir/tests",
        FrontendConfiguratorTestModel.FrontendConfiguratorType.FIR,
        baseClass,
        init
    )
    if (generateFe10) {
        analysisApiTest(
            "analysis/analysis-api-fe10/tests",
            FrontendConfiguratorTestModel.FrontendConfiguratorType.FE10,
            baseClass,
            init
        )
    }
}


internal fun TestGroupSuite.test(
    baseClass: KClass<*>,
    addFe10: Boolean = true,
    init: TestGroup.TestClass.() -> Unit,
) {
    FirAndFe10TestGroup(this, directory = null).test(baseClass, addFe10, init)
}


internal fun TestGroupSuite.group(
    directory: String,
    init: FirAndFe10TestGroup.() -> Unit,
) {
    FirAndFe10TestGroup(this, directory).init()
}


internal fun TestGroupSuite.component(
    directory: String,
    init: FirAndFe10TestGroup.() -> Unit,
) {
    group("components/$directory", init)
}

private fun FirAndFe10TestGroup.analysisApiTest(
    testRoot: String,
    frontendConfiguratorType: FrontendConfiguratorTestModel.FrontendConfiguratorType,
    testClass: KClass<*>,
    init: TestGroup.TestClass.() -> Unit,
) {
    with(suite) {
        val fullTestPath = "analysis/analysis-api/testData" + directory?.let { "/$it" }.orEmpty()
        testGroup(testRoot, fullTestPath) {
            val prefix = when (frontendConfiguratorType) {
                FrontendConfiguratorTestModel.FrontendConfiguratorType.FIR -> "Fir"
                FrontendConfiguratorTestModel.FrontendConfiguratorType.FE10 -> "Fe10"
            }
            val fullPackage = getPackageName(prefix, testClass)

            testClass(
                testClass,
                suiteTestClassName = fullPackage + prefix + getDefaultSuiteTestClassName(testClass.java.simpleName),
                useJunit4 = false
            ) {
                method(FrontendConfiguratorTestModel(frontendConfiguratorType))
                init()
            }
        }
    }
}

private fun getPackageName(prefix: String, testClass: KClass<*>): String {
    val basePrefix = "org.jetbrains.kotlin.analysis.api.${prefix.lowercase()}"
    val packagePrefix = testClass.java.name
        .substringAfter("org.jetbrains.kotlin.analysis.api.impl.base.test.")
        .substringBeforeLast('.', "")

    return if (packagePrefix.isEmpty()) "$basePrefix." else "$basePrefix.$packagePrefix."
}



