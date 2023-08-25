/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators

import org.jetbrains.kotlin.generators.model.*
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.extractTagsFromDirectory
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.concurrent.ForkJoinPool
import java.util.regex.Pattern
import kotlin.reflect.KClass

fun testGroupSuite(
    init: TestGroupSuite.() -> Unit
): TestGroupSuite {
    return TestGroupSuite(DefaultTargetBackendComputer).apply(init)
}

fun TestGroupSuite.forEachTestClassParallel(f: (TestGroup.TestClass) -> Unit) {
    testGroups
        .parallelStream()
        .flatMap { it.testClasses.stream() }
        .sorted(compareByDescending { it.testModels.sumOf { it.methods.size } })
        .forEach(f)
}

class TestGroupSuite(val targetBackendComputer: TargetBackendComputer) {
    private val _testGroups = mutableListOf<TestGroup>()
    val testGroups: List<TestGroup>
        get() = _testGroups

    fun testGroup(
        testsRoot: String,
        testDataRoot: String,
        testRunnerMethodName: String = RunTestMethodModel.METHOD_NAME,
        additionalRunnerArguments: List<String> = emptyList(),
        init: TestGroup.() -> Unit
    ) {
        _testGroups += TestGroup(
            testsRoot,
            testDataRoot,
            testRunnerMethodName,
            additionalRunnerArguments,
            targetBackendComputer = targetBackendComputer
        ).apply(init)
    }
}

class TestGroup(
    private val testsRoot: String,
    val testDataRoot: String,
    val testRunnerMethodName: String,
    val additionalRunnerArguments: List<String> = emptyList(),
    val annotations: List<AnnotationModel> = emptyList(),
    val targetBackendComputer: TargetBackendComputer
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
        val testKClass = T::class
        testClass(testKClass, testKClass.java.name, suiteTestClassName, useJunit4, annotations, init)
    }

    fun testClass(
        testKClass: KClass<*>,
        baseTestClassName: String = testKClass.java.name,
        suiteTestClassName: String = getDefaultSuiteTestClassName(baseTestClassName.substringAfterLast('.')),
        useJunit4: Boolean,
        annotations: List<AnnotationModel> = emptyList(),
        init: TestClass.() -> Unit
    ) {
        _testClasses += TestClass(testKClass, baseTestClassName, suiteTestClassName, useJunit4, annotations, targetBackendComputer).apply(init)
    }

    inner class TestClass(
        val testKClass: KClass<*>,
        val baseTestClassName: String,
        val suiteTestClassName: String,
        val useJunit4: Boolean,
        val annotations: List<AnnotationModel>,
        val targetBackendComputer: TargetBackendComputer
    ) {
        val testDataRoot: String
            get() = this@TestGroup.testDataRoot
        val baseDir: String
            get() = this@TestGroup.testsRoot

        val testModels = ArrayList<TestClassModel>()
        private val methodModels = mutableListOf<MethodModel>()

        fun method(method: MethodModel) {
            methodModels += method
        }

        fun model(
            relativeRootPath: String,
            recursive: Boolean = true,
            excludeParentDirs: Boolean = false,
            extension: String? = "kt", // null string means dir (name without dot)
            pattern: String = if (extension == null) """^([^\.]+)$""" else "^(.+)\\.$extension\$",
            excludedPattern: String? = null,
            testMethod: String = "doTest",
            singleClass: Boolean = false, // if true then tests from subdirectories will be flattened to single class
            testClassName: String? = null, // specific name for generated test class
            // which backend will be used in test. Specifying value may affect some test with
            // directives TARGET_BACKEND/DONT_TARGET_EXACT_BACKEND won't be generated
            targetBackend: TargetBackend? = null,
            excludeDirs: List<String> = listOf(),
            excludeDirsRecursively: List<String> = listOf(),
            filenameStartsLowerCase: Boolean? = null, // assert that file is properly named
            skipIgnored: Boolean = false, // pretty meaningless flag, affects only few test names in one test runner
            deep: Int? = null, // specifies how deep recursive search will follow directory with testdata
        ) {
            val rootFile = File("$testDataRoot/$relativeRootPath")
            val compiledPattern = Pattern.compile(pattern)
            val compiledExcludedPattern = excludedPattern?.let { Pattern.compile(it) }
            val className = testClassName ?: TestGeneratorUtil.fileNameToJavaIdentifier(rootFile)
            val realTargetBackend = targetBackendComputer.compute(targetBackend, testKClass)
            testModels.add(
                if (singleClass) {
                    if (excludeDirs.isNotEmpty()) error("excludeDirs is unsupported for SingleClassTestModel yet")

                    SingleClassTestModel(
                        rootFile, compiledPattern, compiledExcludedPattern, filenameStartsLowerCase, testMethod, className,
                        realTargetBackend, skipIgnored, testRunnerMethodName, additionalRunnerArguments, annotations,
                        extractTagsFromDirectory(rootFile), methodModels
                    )
                } else {
                    SimpleTestClassModel(
                        rootFile, recursive, excludeParentDirs,
                        compiledPattern, compiledExcludedPattern, filenameStartsLowerCase, testMethod, className,
                        realTargetBackend, excludeDirs, excludeDirsRecursively, skipIgnored, testRunnerMethodName, additionalRunnerArguments, deep, annotations,
                        extractTagsFromDirectory(rootFile), methodModels
                    )
                }
            )
        }
    }
}

fun getDefaultSuiteTestClassName(baseTestClassName: String): String {
    require(baseTestClassName.startsWith("Abstract")) { "Doesn't start with \"Abstract\": $baseTestClassName" }
    return baseTestClassName.substringAfter("Abstract") + "Generated"
}
