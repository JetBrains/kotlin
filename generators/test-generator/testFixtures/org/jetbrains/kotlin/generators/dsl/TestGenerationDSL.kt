/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.dsl

import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.*
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.extractTagsFromDirectory
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.regex.Pattern

fun testGroupSuite(
    init: TestGroupSuite.() -> Unit
): TestGroupSuite {
    return TestGroupSuite().apply(init)
}

fun TestGroupSuite.forEachTestClassParallel(f: (TestGroup.TestClass) -> Unit) {
    testGroups
        .parallelStream()
        .flatMap { it.testClasses.stream() }
        .sorted(compareByDescending { it.testModels.sumOf { it.methods.size } })
        .forEach(f)
}

class TestGroupSuite {
    private val _testGroups = mutableListOf<TestGroup>()
    val testGroups: List<TestGroup>
        get() = _testGroups

    fun testGroup(
        testsRoot: String,
        testDataRoot: String,
        testRunnerMethodName: String = MethodGenerator.DEFAULT_RUN_TEST_METHOD_NAME,
        init: TestGroup.() -> Unit
    ) {
        _testGroups += TestGroup(
            testsRoot,
            testDataRoot,
            testRunnerMethodName,
        ).apply(init)
    }
}

class TestGroup(
    private val testsRoot: String,
    val testDataRoot: String,
    val testRunnerMethodName: String,
    val annotations: List<AnnotationModel> = emptyList(),
) {
    private val _testClasses: MutableList<TestClass> = mutableListOf()
    val testClasses: List<TestClass>
        get() = _testClasses

    inline fun <reified T> testClass(
        suiteTestClassName: String = getDefaultSuiteTestClassName(T::class.java.simpleName),
        useJunit4: Boolean = false,
        annotations: List<AnnotationModel> = emptyList(),
        noinline init: TestClass.() -> Unit
    ) {
        val testKClass = T::class.java
        testClass(testKClass, testKClass.name, suiteTestClassName, useJunit4, annotations, init)
    }

    fun testClass(
        testKClass: Class<*>,
        baseTestClassName: String = testKClass.name,
        suiteTestClassName: String = getDefaultSuiteTestClassName(baseTestClassName.substringAfterLast('.')),
        useJunit4: Boolean,
        annotations: List<AnnotationModel> = emptyList(),
        init: TestClass.() -> Unit
    ) {
        _testClasses += TestClass(testKClass, baseTestClassName, suiteTestClassName, useJunit4, annotations).apply(init)
    }

    inner class TestClass(
        val testKClass: Class<*>,
        val baseTestClassName: String,
        val suiteTestClassName: String,
        val useJunit4: Boolean,
        val annotations: List<AnnotationModel>,
    ) {
        val testDataRoot: String
            get() = this@TestGroup.testDataRoot
        val baseDir: String
            get() = this@TestGroup.testsRoot

        val testModels = ArrayList<TestClassModel>()
        private val methodModels = mutableListOf<MethodModel<*>>()

        fun method(method: MethodModel<*>) {
            methodModels += method
        }

        fun modelForDirectoryBasedTest(
            relativePath: String,
            testDirectoryName: String,
            extension: String? = "kt",
            excludeParentDirs: Boolean = false,
            recursive: Boolean = true,
            targetBackend: TargetBackend? = null,
            excludedPattern: String? = null,
        ) {
            model(
                "${relativePath}/${testDirectoryName}",
                extension = extension,
                recursive = recursive,
                excludeParentDirs = excludeParentDirs,
                targetBackend = targetBackend,
                excludedPattern = excludedPattern,
                testClassName = testDirectoryName.replaceFirstChar { it.uppercaseChar() } + testKClass.simpleName,
            )
        }

        fun model(
            relativeRootPath: String = "",
            recursive: Boolean = true,
            excludeParentDirs: Boolean = false,
            extension: String? = "kt", // null string means dir (name without dot)
            pattern: String = if (extension == null) """^([^.]+)$""" else """^(.+)\.$extension$""",
            excludedPattern: String? = null,
            testMethod: String = "doTest",
            testClassName: String? = null, // specific name for generated test class
            targetBackend: TargetBackend? = null, // the parameter is redundant for JUnit5 tests
            excludeDirs: List<String> = listOf(),
            excludeDirsRecursively: List<String> = listOf(),
            skipTestAllFilesCheck: Boolean = false,
        ) {
            val rootFile = File("$testDataRoot/$relativeRootPath")
            val compiledPattern = Pattern.compile(pattern)
            val compiledExcludedPattern = excludedPattern?.let { Pattern.compile(it) }
            val className = testClassName ?: TestGeneratorUtil.fileNameToJavaIdentifier(rootFile)
            val realTargetBackend = targetBackend ?: TargetBackend.ANY
            testModels.add(
                SimpleTestClassModel(
                    rootFile, recursive, excludeParentDirs,
                    compiledPattern, compiledExcludedPattern, testMethod, className,
                    realTargetBackend, excludeDirs, excludeDirsRecursively, testRunnerMethodName, annotations,
                    extractTagsFromDirectory(rootFile), methodModels, skipTestAllFilesCheck
                )
            )
        }
    }
}

fun getDefaultSuiteTestClassName(baseTestClassName: String): String {
    require(baseTestClassName.startsWith("Abstract")) { "Doesn't start with \"Abstract\": $baseTestClassName" }
    return baseTestClassName.substringAfter("Abstract") + "Generated"
}
