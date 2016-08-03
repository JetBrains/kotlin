/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.code

import com.intellij.openapi.util.io.FileUtil
import junit.framework.TestCase
import java.io.File
import java.util.*
import java.util.regex.Pattern

class ModulesDependenciesTest : TestCase() {
    val COMPILER_TESTS_MODULE_FILE = File("compiler/tests/compiler-tests.iml")
    val GENERATORS_MODULE_FILE = File("generators/generators.iml")
    val COMPILER_TESTS_JAVA8_MODULE_FILE = File("compiler/tests-java8/compiler-tests-java8.iml")
    val NON_COMPILER_TESTS_MODULE_FILE = File("non-compiler-tests/non-compiler-tests.iml")

    val COMPILER_TESTS_MODULE_NAME = COMPILER_TESTS_MODULE_FILE.nameWithoutExtension
    val MODULES_CAN_DEPEND_ON_COMPILER_TESTS = listOf(COMPILER_TESTS_JAVA8_MODULE_FILE, GENERATORS_MODULE_FILE)

    fun testModulesPresent() {
        val modules = listOf(
                COMPILER_TESTS_MODULE_FILE, GENERATORS_MODULE_FILE, COMPILER_TESTS_JAVA8_MODULE_FILE, NON_COMPILER_TESTS_MODULE_FILE
        )

        for (module in modules) {
            TestCase.assertTrue("Module $module was moved or renamed without update in this test", module.exists())
        }
    }

    fun testNoCompilerModuleReferences() {
        val badModulesList = ArrayList<File>()

        for (moduleFile in FileUtil.findFilesByMask(Pattern.compile(".+\\.iml"), File("."))) {
            if (MODULES_CAN_DEPEND_ON_COMPILER_TESTS.any { FileUtil.isAncestor(it, moduleFile, false) }) continue

            val moduleText = moduleFile.readText()
            if (moduleText.contains("\"$COMPILER_TESTS_MODULE_NAME\"")) {
                badModulesList.add(moduleFile)
            }
        }

        TestCase.assertTrue("Number of modules have dependencies to module $COMPILER_TESTS_MODULE_NAME. Such dependencies can cause multiple " +
                            "tests execution on teamcity when running configuration with 'Search tests across module dependencies' enabled:\n" +
                            badModulesList.joinToString(separator = "\n"),
                            badModulesList.isEmpty())
    }

    fun testNoModulesFromOtherTestConfigurationsInNonCompilerTests() {
        val FORBIDDEN_MODULE_NAMES = listOf(COMPILER_TESTS_JAVA8_MODULE_FILE, COMPILER_TESTS_MODULE_FILE).map { it.nameWithoutExtension }

        val moduleText = NON_COMPILER_TESTS_MODULE_FILE.readText()
        val nonCompilerTestsHasForbiddenDependencies = FORBIDDEN_MODULE_NAMES.none { moduleText.contains(it) }

        TestCase.assertTrue("${NON_COMPILER_TESTS_MODULE_FILE.nameWithoutExtension} has a dependency to modules that can depend on " +
                            "$COMPILER_TESTS_MODULE_NAME module. This can cause multiple execution of compiler tests on teamcity.\n" +
                            "Check modules:\n${MODULES_CAN_DEPEND_ON_COMPILER_TESTS.joinToString("\n")}",
                            nonCompilerTestsHasForbiddenDependencies)
    }
}
