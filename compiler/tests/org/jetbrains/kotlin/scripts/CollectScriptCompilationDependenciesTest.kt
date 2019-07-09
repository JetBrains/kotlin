/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripts

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

private const val testDataPath = "compiler/testData/script/collectDependencies"

class CollectScriptCompilationDependenciesTest : KtUsefulTestCase() {

    fun testCascadeImport() {
        runTest("imp_imp_leaf.req1.kts", listOf("imp_leaf.req1.kts", "leaf.req1.kts"))
    }

    fun testImportTwice() {
        runTest("imp_leaf_twice.req1.kts", listOf("leaf.req1.kts"))
    }

    fun testImportDiamond() {
        runTest("imp_leaf_and_imp_imp_leaf.req1.kts", listOf("imp_leaf.req1.kts", "leaf.req1.kts"))
    }

    fun testDirectImportCycle() {
        runTest("imp_self.req1.kts", emptyList())
    }

    fun testIndirectImportCycle() {
        runTest("imp_cycle_1.req1.kts", listOf("imp_cycle_2.req1.kts"))
    }

    fun testImportWithDependenciesAdded() {
        runTest(
            "imp_leaf_with_deps.req1.kts",
            listOf("leaf_with_deps_1.req1.kts", "leaf_with_deps_2.req1.kts"),
            listOf(File("someDependency1.jar"), File("someDependency2.jar"))
        )
    }

    private fun runTest(scriptFile: String, expectedDependencies: List<String>, classPath: List<File> = emptyList()) {
        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.NO_KOTLIN_REFLECT, TestJdkKind.MOCK_JDK).apply {
            add(
                ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
                ScriptDefinition.FromTemplate(
                    ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration),
                    TestScriptWithRequire::class,
                    ScriptDefinition::class
                )
            )

            addKotlinSourceRoot(File(testDataPath, scriptFile).path)

            loadScriptingPlugin(this)
        }
        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        val expectedSources = (expectedDependencies + scriptFile).sorted()
        val actualSources = environment.getSourceFiles().map { it.name }.sorted()

        TestCase.assertEquals(expectedSources, actualSources)

        if (classPath.isNotEmpty()) {

            val actualClasspath = environment.configuration.jvmClasspathRoots

            TestCase.assertTrue("expect that $actualClasspath contains $classPath", actualClasspath.containsAll(classPath))
        }
    }
}