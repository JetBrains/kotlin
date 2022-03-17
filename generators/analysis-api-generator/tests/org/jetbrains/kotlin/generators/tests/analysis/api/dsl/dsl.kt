/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api.dsl

import org.jetbrains.kotlin.analysis.api.descriptors.test.KtFe10AnalysisApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.fir.FirAnalysisApiDependentModeTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.fir.FirAnalysisApiNormalModeTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.fir.utils.libraries.binary.LibraryAnalysisApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.fir.utils.libraries.source.LibrarySourceAnalysisApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneModeConfiguratorService
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestConfiguratorService
import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.getDefaultSuiteTestClassName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import kotlin.reflect.KClass

internal data class FirAndFe10TestGroup(
    val suite: TestGroupSuite,
    val directory: String?,
    val testModuleKinds: List<TestModuleKind>,
)

internal sealed class TestModuleKind(val componentName: String) {
    object SOURCE : TestModuleKind("source")
    object LIBRARY : TestModuleKind("library")
    object LIBRARY_SOURCE : TestModuleKind("librarySource")

    object STANDALONE_MODE : TestModuleKind("StandaloneMode")

    companion object {
        val SOURCE_ONLY by lazy(LazyThreadSafetyMode.NONE) { listOf(SOURCE) }
        val SOURCE_AND_LIBRARY_SOURCE by lazy(LazyThreadSafetyMode.NONE) { listOf(SOURCE, LIBRARY_SOURCE) }
    }
}

internal enum class AnalysisMode(val suffix: String) {
    NORMAL("NormalMode"),
    DEPENDENT("DependentMode"),
}

internal enum class Frontend(val prefix: String) {
    FIR("Fir"),
    FE10("Fe10"),
}

internal fun FirAndFe10TestGroup.test(
    baseClass: KClass<*>,
    generateFe10: Boolean = true,
    analysisModesForFir: List<AnalysisMode> = listOf(AnalysisMode.NORMAL, AnalysisMode.DEPENDENT),
    init: TestGroup.TestClass.(module: TestModuleKind) -> Unit,
) {
    analysisApiTest(
        "analysis/analysis-api-fir/tests",
        Frontend.FIR,
        baseClass,
        analysisModesForFir,
        init
    )
    if (generateFe10) {
        analysisApiTest(
            "analysis/analysis-api-fe10/tests",
            Frontend.FE10,
            baseClass,
            listOf(AnalysisMode.NORMAL),
            init
        )
    }
}

internal fun TestGroupSuite.test(
    baseClass: KClass<*>,
    addFe10: Boolean = true,
    testModuleKinds: List<TestModuleKind> = TestModuleKind.SOURCE_ONLY,
    analysisModesForFir: List<AnalysisMode> = listOf(AnalysisMode.NORMAL, AnalysisMode.DEPENDENT),
    init: TestGroup.TestClass.(module: TestModuleKind) -> Unit,
) {
    FirAndFe10TestGroup(this, directory = null, testModuleKinds).test(baseClass, addFe10, analysisModesForFir, init)
}

internal fun TestGroupSuite.group(
    directory: String,
    testModuleKinds: List<TestModuleKind> = TestModuleKind.SOURCE_ONLY,
    init: FirAndFe10TestGroup.() -> Unit,
) {
    FirAndFe10TestGroup(this, directory, testModuleKinds).init()
}

internal fun TestGroupSuite.component(
    directory: String,
    init: FirAndFe10TestGroup.() -> Unit,
) {
    group("components/$directory", init = init)
}

private fun FirAndFe10TestGroup.analysisApiTest(
    testRoot: String,
    frontend: Frontend,
    testClass: KClass<*>,
    analysisModes: List<AnalysisMode>,
    init: TestGroup.TestClass.(module: TestModuleKind) -> Unit,
) {
    with(suite) {
        val fullTestPath = "analysis/analysis-api/testData" + directory?.let { "/$it" }.orEmpty()
        testGroup(testRoot, fullTestPath) {
            for (testModuleKind in testModuleKinds) {
                for (analysisMode in analysisModes) {
                    analysisApiTestClass(frontend, testModuleKind, analysisMode, testClass, init)
                }
            }
        }
    }
}

private fun TestGroup.analysisApiTestClass(
    frontend: Frontend,
    moduleKind: TestModuleKind,
    analysisMode: AnalysisMode,
    testClass: KClass<*>,
    init: TestGroup.TestClass.(module: TestModuleKind) -> Unit
) {
    val fullPackage = getPackageName(frontend.prefix, testClass)

    val suiteTestClassName = buildString {
        append(fullPackage)
        append(frontend.prefix)
        append(moduleKind.componentName.capitalizeAsciiOnly())
        append(analysisMode.suffix)

        append(getDefaultSuiteTestClassName(testClass.java.simpleName))
    }

    getDefaultSuiteTestClassName(testClass.java.simpleName)

    val configurator = createConfigurator(frontend, moduleKind, analysisMode)

    testClass(
        testClass,
        suiteTestClassName = suiteTestClassName,
        useJunit4 = false
    ) {
        method(FrontendConfiguratorTestModel(configurator))
        init(moduleKind)
    }
}

internal fun createConfigurator(
    frontend: Frontend,
    moduleKind: TestModuleKind,
    analysisMode: AnalysisMode,
): KClass<out AnalysisApiTestConfiguratorService> = when (frontend) {
    Frontend.FIR -> when (moduleKind) {
        TestModuleKind.SOURCE -> when (analysisMode) {
            AnalysisMode.NORMAL -> FirAnalysisApiNormalModeTestConfiguratorService::class
            AnalysisMode.DEPENDENT -> FirAnalysisApiDependentModeTestConfiguratorService::class
        }

        TestModuleKind.LIBRARY -> {
            checkNormalMode(frontend, moduleKind, analysisMode)
            LibraryAnalysisApiTestConfiguratorService::class
        }

        TestModuleKind.LIBRARY_SOURCE -> {
            checkNormalMode(frontend, moduleKind, analysisMode)
            LibrarySourceAnalysisApiTestConfiguratorService::class
        }

        TestModuleKind.STANDALONE_MODE -> {
            checkNormalMode(frontend, moduleKind, analysisMode)
            StandaloneModeConfiguratorService::class
        }
    }

    Frontend.FE10 -> when (moduleKind) {
        TestModuleKind.SOURCE -> {
            checkNormalMode(frontend, moduleKind, analysisMode)
            KtFe10AnalysisApiTestConfiguratorService::class
        }

        TestModuleKind.LIBRARY -> TODO("TestModuleKind.LIBRARY is unsupported for fe10")
        TestModuleKind.LIBRARY_SOURCE -> TODO("TestModuleKind.LIBRARY_SOURCE is unsupported for fe10")
        TestModuleKind.STANDALONE_MODE -> TODO("TestModuleKind.STANDALONE_MODE is unsupported for fe10")
    }
}

private fun checkNormalMode(
    frontend: Frontend,
    moduleKind: TestModuleKind,
    analysisMode: AnalysisMode
) {
    if (analysisMode == AnalysisMode.DEPENDENT) {
        TODO("${AnalysisMode.DEPENDENT} is not supported for $frontend with $moduleKind")
    }
}


private fun getPackageName(prefix: String, testClass: KClass<*>): String {
    val basePrefix = "org.jetbrains.kotlin.analysis.api.${prefix.lowercase()}.test.cases"
    val packagePrefix = testClass.java.name
        .substringAfter("org.jetbrains.kotlin.analysis.api.impl.base.test.")
        .substringBeforeLast('.', "")

    return if (packagePrefix.isEmpty()) "$basePrefix." else "$basePrefix.$packagePrefix."
}



