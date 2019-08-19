/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator

import junit.framework.TestCase
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.*
import java.util.regex.Pattern

class TestGroup(private val testsRoot: String, val testDataRoot: String, val testRunnerMethodName: String) {
    inline fun <reified T : TestCase> testClass(
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
            skipIgnored: Boolean = false,
            deep: Int? = null
        ) {
            val rootFile = File("$testDataRoot/$relativeRootPath")
            val compiledPattern = Pattern.compile(pattern)
            val className = testClassName ?: TestGeneratorUtil.fileNameToJavaIdentifier(rootFile)
            testModels.add(
                if (singleClass) {
                    if (excludeDirs.isNotEmpty()) error("excludeDirs is unsupported for SingleClassTestModel yet")
                    SingleClassTestModel(
                        rootFile, compiledPattern, filenameStartsLowerCase, testMethod, className, targetBackend,
                        skipIgnored, testRunnerMethodName
                    )
                } else {
                    SimpleTestClassModel(
                        rootFile, recursive, excludeParentDirs,
                        compiledPattern, filenameStartsLowerCase, testMethod, className,
                        targetBackend, excludeDirs, skipIgnored, testRunnerMethodName, deep
                    )
                }
            )
        }
    }
}

fun testGroup(
    testsRoot: String,
    testDataRoot: String,
    testRunnerMethodName: String = RunTestMethodModel.METHOD_NAME,
    init: TestGroup.() -> Unit
) {
    TestGroup(testsRoot, testDataRoot, testRunnerMethodName).init()
}

fun getDefaultSuiteTestClassName(baseTestClassName: String): String {
    require(baseTestClassName.startsWith("Abstract")) { "Doesn't start with \"Abstract\": $baseTestClassName" }
    return baseTestClassName.substringAfter("Abstract") + "Generated"
}
