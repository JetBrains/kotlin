/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.generators.tests.generator

import junit.framework.TestCase
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.lang.IllegalArgumentException
import java.util.ArrayList
import java.util.regex.Pattern

class TestGroup(private val testsRoot: String, val testDataRoot: String) {
    inline fun <reified T: TestCase> testClass(
            suiteTestClassName: String = getDefaultSuiteTestClassName(T::class.java.simpleName),
            noinline init: TestClass.() -> Unit
    ) {
        testClass(T::class.java.name, suiteTestClassName, init)
    }

    fun testClass(
            baseTestClassName: String,
            suiteTestClassName: String = getDefaultSuiteTestClassName(baseTestClassName.substringAfterLast('.')),
            init: TestClass.() -> Unit
    ) {
        TestGenerator(
                testsRoot,
                suiteTestClassName,
                baseTestClassName,
                TestClass().apply(init).testModels
        ).generateAndSave()
    }

    inner class TestClass {
        val testModels = ArrayList<TestClassModel>()

        fun model(
                relativeRootPath: String,
                recursive: Boolean = true,
                excludeParentDirs: Boolean = false,
                extension: String? = "kt", // null string means dir (name without dot)
                pattern: String = if (extension == null) """^([^\.]+)$""" else "^(.+)\\.$extension\$",
                testMethod: String = "doTest",
                singleClass: Boolean = false,
                testClassName: String? = null,
                targetBackend: TargetBackend = TargetBackend.ANY,
                excludeDirs: List<String> = listOf(),
                filenameStartsLowerCase: Boolean? = null,
                skipIgnored: Boolean = false
        ) {
            val rootFile = File(testDataRoot + "/" + relativeRootPath)
            val compiledPattern = Pattern.compile(pattern)
            val className = testClassName ?: TestGeneratorUtil.fileNameToJavaIdentifier(rootFile)
            testModels.add(
                    if (singleClass) {
                        if (excludeDirs.isNotEmpty()) error("excludeDirs is unsupported for SingleClassTestModel yet")
                        SingleClassTestModel(rootFile, compiledPattern, filenameStartsLowerCase, testMethod, className, targetBackend,
                                             skipIgnored)
                    }
                    else {
                        SimpleTestClassModel(rootFile, recursive, excludeParentDirs,
                                             compiledPattern, filenameStartsLowerCase, testMethod, className,
                                             targetBackend, excludeDirs, skipIgnored)
                    }
            )
        }
    }
}

fun testGroup(testsRoot: String, testDataRoot: String, init: TestGroup.() -> Unit) {
    TestGroup(testsRoot, testDataRoot).init()
}

fun getDefaultSuiteTestClassName(baseTestClassName: String): String {
    if (!baseTestClassName.startsWith("Abstract")) {
        throw IllegalArgumentException("Doesn't start with \"Abstract\": $baseTestClassName")
    }
    return baseTestClassName.substringAfter("Abstract") + "Generated"
}
