/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api.dsl

import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiMode
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.defaultExtension
import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.getDefaultSuiteTestClassName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import kotlin.reflect.KClass

internal class AnalysisApiTestGroup(
    private val generator: AnalysisApiTestGenerator,
    private val groupFilter: TestFilter,
    private val directory: String?,
) {
    fun group(directory: String? = null, filter: TestFilter = { true }, init: AnalysisApiTestGroup.() -> Unit) {
        AnalysisApiTestGroup(
            generator,
            groupFilter and filter,
            listOfNotNull(this.directory, directory).joinToString(separator = "/")
        ).init()
    }

    fun suiteBasedTests(init: TestGroupSuite.() -> Unit) {
        generator.suite.init()
    }

    fun test(
        testClass: KClass<*>,
        filter: TestFilter = { true },
        init: TestGroup.TestClass.(data: AnalysisApiTestConfiguratorFactoryData) -> Unit,
    ) {
        with(generator.suite) {
            val fullTestPath = "analysis/analysis-api/testData" + directory?.let { "/$it" }.orEmpty()

            AnalysisApiConfiguratorFactoryProvider.allPossibleFactoryDataList.filter(groupFilter).filter(filter)
                .groupBy { AnalysisApiConfiguratorFactoryProvider.getTestPath(it) }
                .forEach { (testRoot, datas) ->
                    if (testRoot == null) return@forEach
                    testGroup(testRoot, fullTestPath) {
                        datas.forEach { data ->
                            analysisApiTestClass(data, testClass, init)
                        }
                    }
                }
        }
    }
}

internal fun TestGroup.TestClass.model(
    data: AnalysisApiTestConfiguratorFactoryData,
    relativeRootPath: String,
    excludeDirsRecursively: List<String> = listOf(),
    excludedPattern: String? = null,
) {
    model(
        relativeRootPath = relativeRootPath,
        extension = data.defaultExtension(),
        excludeDirsRecursively = excludeDirsRecursively,
        excludedPattern = excludedPattern,
    )
}

private fun TestGroup.analysisApiTestClass(
    data: AnalysisApiTestConfiguratorFactoryData,
    testClass: KClass<*>,
    init: TestGroup.TestClass.(data: AnalysisApiTestConfiguratorFactoryData) -> Unit
) {
    val factory = AnalysisApiConfiguratorFactoryProvider.getFactory(data) ?: return

    val fullPackage = getPackageName(data, testClass)

    val suiteTestClassName = buildString {
        append(fullPackage)
        append(getTestNameSuffix(data))
        append(getDefaultSuiteTestClassName(testClass.java.simpleName))
    }

    getDefaultSuiteTestClassName(testClass.java.simpleName)


    testClass(
        testClass,
        suiteTestClassName = suiteTestClassName,
        useJunit4 = false
    ) {
        method(FrontendConfiguratorTestModel(factory::class, data))
        init(data)
    }
}

private fun getTestNameSuffix(data: AnalysisApiTestConfiguratorFactoryData): String {
    return buildString {
        append(data.frontend.suffix.capitalizeAsciiOnly())
        append(data.analysisApiMode.suffix.capitalizeAsciiOnly());
        append(data.analysisSessionMode.suffix.capitalizeAsciiOnly()); append("Analysis")
        append(data.moduleKind.suffix.capitalizeAsciiOnly()); append("Module")
    }
}

private fun getPackageName(data: AnalysisApiTestConfiguratorFactoryData, testClass: KClass<*>): String {
    val basePrefix = buildString {
        append("org.jetbrains.kotlin.analysis.api.")
        if (data.analysisApiMode == AnalysisApiMode.Standalone) {
            append("standalone.")
        }
        append(data.frontend.suffix.lowercase())
        append(".test.cases.generated")
    }
    val packagePrefix = "cases." + testClass.java.name
        .substringAfter("test.cases.")
        .substringBeforeLast('.', "")

    return if (packagePrefix.isEmpty()) "$basePrefix." else "$basePrefix.$packagePrefix."
}