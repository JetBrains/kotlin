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

class TestGroup(
    private val testsRoot: String,
    val testDataRoot: String,
    val testRunnerMethodName: String,
    val additionalRunnerArguments: List<String> = emptyList(),
    val annotations: List<AnnotationModel> = emptyList()
) {
    inline fun <reified T : TestCase> testClass(
        suiteTestClassName: String = getDefaultSuiteTestClassName(T::class.java.simpleName),
        useJunit4: Boolean = false,
        annotations: List<AnnotationModel> = emptyList(),
        noinline init: TestClass.() -> Unit
    ) {
        testClass(T::class.java.name, suiteTestClassName, useJunit4, annotations, init)
    }

    fun testClass(
        baseTestClassName: String,
        suiteTestClassName: String = getDefaultSuiteTestClassName(baseTestClassName.substringAfterLast('.')),
        useJunit4: Boolean,
        annotations: List<AnnotationModel> = emptyList(),
        init: TestClass.() -> Unit
    ) {
        TestGenerator(
            testsRoot,
            suiteTestClassName,
            baseTestClassName,
            TestClass(annotations).apply(init).testModels,
            useJunit4
        ).generateAndSave()
    }

    inner class TestClass(val annotations: List<AnnotationModel>) {
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
                        skipIgnored, testRunnerMethodName, additionalRunnerArguments, annotations
                    )
                } else {
                    SimpleTestClassModel(
                        rootFile, recursive, excludeParentDirs,
                        compiledPattern, filenameStartsLowerCase, testMethod, className,
                        targetBackend, excludeDirs, skipIgnored, testRunnerMethodName, additionalRunnerArguments, deep, annotations
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
    additionalRunnerArguments: List<String> = emptyList(),
    init: TestGroup.() -> Unit
) {
    TestGroup(testsRoot, testDataRoot, testRunnerMethodName, additionalRunnerArguments).init()
}

fun getDefaultSuiteTestClassName(baseTestClassName: String): String {
    require(baseTestClassName.startsWith("Abstract")) { "Doesn't start with \"Abstract\": $baseTestClassName" }
    return baseTestClassName.substringAfter("Abstract") + "Generated"
}
