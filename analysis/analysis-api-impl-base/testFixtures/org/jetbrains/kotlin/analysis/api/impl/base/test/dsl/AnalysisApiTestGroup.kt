/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api.dsl

import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiMode
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.defaultExtension
import org.jetbrains.kotlin.generators.dsl.TestGroup
import org.jetbrains.kotlin.generators.dsl.TestGroupSuite
import org.jetbrains.kotlin.generators.dsl.getDefaultSuiteTestClassName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

class AnalysisApiTestGroup(
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

    inline fun <reified T : Any> test(
        noinline filter: TestFilter = { true },
        noinline init: TestGroup.TestClass.(data: AnalysisApiTestConfiguratorFactoryData) -> Unit,
    ) {
        test(T::class.java, filter, init)
    }

    fun test(
        testClass: Class<*>,
        filter: TestFilter = { true },
        init: TestGroup.TestClass.(data: AnalysisApiTestConfiguratorFactoryData) -> Unit,
    ) {
        with(generator.suite) {
            val fullTestPath = "analysis/analysis-api/testData" + directory?.let { "/$it" }.orEmpty()

            ALL_POSSIBLE_FACTORY_DATA_LIST.filter(groupFilter).filter(filter)
                .groupBy { it.testPath }
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

    private fun TestGroup.analysisApiTestClass(
        data: AnalysisApiTestConfiguratorFactoryData,
        testClass: Class<*>,
        init: TestGroup.TestClass.(data: AnalysisApiTestConfiguratorFactoryData) -> Unit
    ) {
        val factory = findMatchingFactory(data) ?: return

        val fullPackage = getPackageName(data, testClass)

        val suiteTestClassName = buildString {
            append(fullPackage)
            append(getTestNameSuffix(data))
            append(getDefaultSuiteTestClassName(testClass.simpleName))
        }

        getDefaultSuiteTestClassName(testClass.simpleName)


        testClass(
            testClass,
            suiteTestClassName = suiteTestClassName,
        ) {
            method(FrontendConfiguratorTestModel(factory::class, data))
            init(data)
        }
    }

    private fun findMatchingFactory(data: AnalysisApiTestConfiguratorFactoryData): AnalysisApiTestConfiguratorFactory? {
        val supportedFactories = generator.configuratorFactories.filter { it.supportMode(data) }
        check(supportedFactories.size <= 1) {
            buildString {
                append("For $data")
                append(" expected no more than 1 supported ")
                append(AnalysisApiTestConfiguratorFactory::class.simpleName)
                append(" but ${supportedFactories.size} found ")
                append(supportedFactories.joinToString(prefix = "[", postfix = "]") { it::class.simpleName!! })
            }

        }
        return supportedFactories.singleOrNull()
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

private fun getTestNameSuffix(data: AnalysisApiTestConfiguratorFactoryData): String {
    return buildString {
        append(data.frontend.suffix.capitalizeAsciiOnly())
        append(data.analysisApiMode.suffix.capitalizeAsciiOnly());
        append(data.analysisSessionMode.suffix.capitalizeAsciiOnly()); append("Analysis")
        append(data.moduleKind.suffix.capitalizeAsciiOnly()); append("Module")
    }
}

private fun getPackageName(data: AnalysisApiTestConfiguratorFactoryData, testClass: Class<*>): String {
    val basePrefix = buildString {
        append("org.jetbrains.kotlin.analysis.api.")
        if (data.analysisApiMode == AnalysisApiMode.Standalone) {
            append("standalone.")
        }
        append(data.frontend.suffix.lowercase())
        append(".test.cases.generated")
    }
    val packagePrefix = "cases." + testClass.name
        .substringAfter("test.cases.")
        .substringBeforeLast('.', "")

    return if (packagePrefix.isEmpty()) "$basePrefix." else "$basePrefix.$packagePrefix."
}

private val AnalysisApiTestConfiguratorFactoryData.testPath: String?
    get() = when (frontend) {
        FrontendKind.Fir if analysisApiMode == AnalysisApiMode.Ide -> "analysis/analysis-api-fir/tests-gen"
        FrontendKind.Fe10 if analysisApiMode == AnalysisApiMode.Ide -> "analysis/analysis-api-fe10/tests-gen"
        FrontendKind.Fir if analysisApiMode == AnalysisApiMode.Standalone -> "analysis/analysis-api-standalone/tests-gen"
        else -> null
    }

private val ALL_POSSIBLE_FACTORY_DATA_LIST: List<AnalysisApiTestConfiguratorFactoryData> = buildList {
    FrontendKind.entries.forEach { frontend ->
        TestModuleKind.entries.forEach { moduleKind ->
            AnalysisSessionMode.entries.forEach { analysisSessionMode ->
                AnalysisApiMode.entries.forEach { analysisApiMode ->
                    add(AnalysisApiTestConfiguratorFactoryData(frontend, moduleKind, analysisSessionMode, analysisApiMode))
                }
            }
        }
    }
}
