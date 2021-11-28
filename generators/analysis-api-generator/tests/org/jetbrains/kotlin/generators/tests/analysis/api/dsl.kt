/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.TestGroupSuite
import kotlin.reflect.KClass

internal class FirAndFe10TestGroup(
    val suite: TestGroupSuite,
    val directory: String?
)

internal fun FirAndFe10TestGroup.test(
    fir: KClass<*>?,
    fe10: KClass<*>?,
    init: TestGroup.TestClass.() -> Unit,
) {
    if (fir != null) {
        analysisApiTest("analysis/analysis-api-fir/tests", fir, init)
    }

    if (fe10 != null) {
        analysisApiTest("analysis/analysis-api-fe10/tests", fe10, init)
    }
}

internal fun TestGroupSuite.test(
    fir: KClass<*>?,
    fe10: KClass<*>?,
    init: TestGroup.TestClass.() -> Unit,
) {
    FirAndFe10TestGroup(this, directory = null).test(fir, fe10, init)
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
    testClass: KClass<*>,
    init: TestGroup.TestClass.() -> Unit,
) {
    with(suite) {
        val fullTestPath = "analysis/analysis-api/testData" + directory?.let { "/$it" }.orEmpty()
        testGroup(testRoot, fullTestPath) {
            testClass(testClass, useJunit4 = false) {
                init()
            }
        }
    }
}



