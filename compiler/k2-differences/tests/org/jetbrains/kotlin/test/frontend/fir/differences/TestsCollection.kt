/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.differences

import kotlinx.serialization.Serializable
import java.io.File

val knownTestDataCategories = listOf(
    "testData/codegen",
    "testData/cli",
    "testData/compileKotlinAgainstJava",
    "testData/ir/interpreter",
    "testData/lightClasses",
    "testData/diagnostics", // try collecting diagnostic-absence-checking non-alongside tests here
    "analysis-tests/testData/resolve",
    "kotlin-native/backend.native/tests",
    "ir/irText",
    "analysis/low-level-api-fir/testdata",
    "analysis/symbol-light-classes/testData",
    "analysis/decompiled/decompiler-to-file-stubs/testData",
    "analysis/analysis-api/testData",
    "native/commonizer/testData",
    "native/native.tests/testData",
    "prepare/compiler-embeddable/testData",
    "kotlin-native/prepare/kotlin-native-embeddable-compiler/testData",
    "js/js.translator/testData",
    "jps/jps-plugin/testData",
    "libraries/tools/kotlinp/testData",
    "libraries/scripting/jsr223-test/testData",
    "libraries/scripting/jvm-host-test/testData",
    "idea/testData",
    "compiler/fir/raw-fir/psi2fir/testData",
    "compiler/tests-spec/testData/psi",
    "compiler/incremental-compilation-impl/testData",
    "compiler/visualizer/testData",
    "compiler/testData", // other uncategorized
)

val testDataFolders = listOf(
    "testData".name,
    "testdata".name,
    "kotlin-native/backend.native/tests".substring,
)

val definitelyFirIdenticalTestData = listOf(
    "/codegen/box".substring,
)

val nonTestDataKotlinFolders = listOf(
    "src".name,
    "test".name,
    "tests".name,
    "tests-gen".name,
    "runtime".name,
    "builtins".name,
    "gen".name,

    "/kotlin-native/Interop/Indexer/prebuilt".substring,
    "/libraries/stdlib/jdk8/moduleTest".substring,
    "/libraries/stdlib/wasm/stubs".substring,
    "/libraries/stdlib/wasm/internal".substring,
    "/libraries/stdlib/jvm/testLongRunning".substring,
    "/libraries/stdlib/jvm/compileOnly".substring,
    "/libraries/stdlib/api".substring,
    "/generators".substring,
)

val irrelevantFolders = listOf(
    "build".name,
    "gradle".name,
    "resources".name,
    "testResources".name,
)

@Serializable
class TestsCollection {
    val alongsideIdenticalTests = mutableListOf<String>()
    val alongsideNonIdenticalTests = mutableListOf<String>()
    val categories = mutableMapOf<String, MutableList<String>>()
    val pluginTests = mutableMapOf<String, MutableList<String>>()
    val uncategorizedTests = mutableListOf<String>()
}

val File.analogousK2File: File
    get() = when {
        path.contains(".fir.") -> error("This is already a definitely K2 file")
        else -> File(path.replace(".kt", ".fir.kt"))
    }

val File.hasAnalogousK2File get() = analogousK2File.isFile

fun collectTestsStats(projectDirectory: File): TestsCollection {
    val tests = TestsCollection()

    projectDirectory.forEachChildRecursively(
        shouldIgnoreDirectory = { file ->
            status.loading("Traversing " + file.path.replace(projectDirectory.path, ""), probability = 0.01)

            val isInsideTestData = file.conforms(testDataFolders)
            val isIrrelevantTest = isInsideTestData && file.conforms(definitelyFirIdenticalTestData)
            val isIrrelevantNonTest = !isInsideTestData
                    && (file.name.startsWith(".") || file.conforms(irrelevantFolders) || file.conforms(nonTestDataKotlinFolders))

            isIrrelevantTest || isIrrelevantNonTest
        },
        action = { file ->
            status.loading("Traversing " + file.path.replace(projectDirectory.path, ""), probability = 0.01)

            val willBeCheckedWithTheCorrespondingK1File = file.name.contains(".fir.") || file.name.contains(".reversed.")
            val isKotlinFile = file.name.endsWith(".kt")

            if (!isKotlinFile || willBeCheckedWithTheCorrespondingK1File) {
                return@forEachChildRecursively
            }

            val text = file.readText()

            val targetCollection = when {
                file.hasAnalogousK2File -> tests.alongsideNonIdenticalTests
                "// FIR_IDENTICAL" in text -> tests.alongsideIdenticalTests
                knownTestDataCategories.any { it in file.path } -> {
                    val category = knownTestDataCategories.find { it in file.path } ?: error("any {} succeeded, but find {} did not")
                    tests.categories.getOrPut(category) { mutableListOf() }
                }
                "/plugins/" in file.path -> {
                    val plugin = extractPluginNameFrom(file.path)
                    tests.pluginTests.getOrPut(plugin) { mutableListOf() }
                }
                else -> tests.uncategorizedTests
            }

            targetCollection.add(file.path)
        },
    )

    status.doneSilently("Test data collected")
    return tests
}

fun extractPluginNameFrom(path: String): String {
    val pathAfterPlugins = path.split("/plugins/").last()
    val testDataSubstringIndex = pathAfterPlugins.indexOf("/testData")

    if (testDataSubstringIndex == -1) {
        error("Please, name test data root folders as `testData` within the plugins directory")
    }

    return pathAfterPlugins.substring(0, testDataSubstringIndex)
}
