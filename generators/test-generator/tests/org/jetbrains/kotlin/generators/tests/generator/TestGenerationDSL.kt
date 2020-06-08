/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator

import junit.framework.TestCase
import org.jetbrains.kotlin.generators.tests.generator.InconsistencyChecker.Companion.hasDryRunArg
import org.jetbrains.kotlin.generators.tests.generator.InconsistencyChecker.Companion.inconsistencyChecker
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.*
import java.util.regex.Pattern

class TestGroup(
    private val testsRoot: String,
    val testDataRoot: String,
    val testRunnerMethodName: String,
    val additionalRunnerArguments: List<String> = emptyList(),
    val annotations: List<AnnotationModel> = emptyList(),
    private val dryRun: Boolean = false
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
        val testGenerator = TestGenerator(
            testsRoot,
            suiteTestClassName,
            baseTestClassName,
            TestClass(annotations).apply(init).testModels,
            useJunit4
        )
        if (testGenerator.generateAndSave(dryRun)) {
            inconsistencyChecker(dryRun).add(testGenerator.testSourceFilePath)
        }
    }

    inner class TestClass(val annotations: List<AnnotationModel>) {
        val testModels = ArrayList<TestClassModel>()

        fun model(
            relativeRootPath: String,
            recursive: Boolean = true,
            excludeParentDirs: Boolean = false,
            extension: String? = "kt", // null string means dir (name without dot)
            pattern: String = if (extension == null) """^([^\.]+)$""" else "^(.+)\\.$extension\$",
            excludedPattern: String? = null,
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
            val compiledExcludedPattern = excludedPattern?.let { Pattern.compile(it) }
            val className = testClassName ?: TestGeneratorUtil.fileNameToJavaIdentifier(rootFile)
            testModels.add(
                if (singleClass) {
                    if (excludeDirs.isNotEmpty()) error("excludeDirs is unsupported for SingleClassTestModel yet")
                    SingleClassTestModel(
                        rootFile, compiledPattern, compiledExcludedPattern, filenameStartsLowerCase, testMethod, className,
                        targetBackend, skipIgnored, testRunnerMethodName, additionalRunnerArguments, annotations
                    )
                } else {
                    SimpleTestClassModel(
                        rootFile, recursive, excludeParentDirs,
                        compiledPattern, compiledExcludedPattern, filenameStartsLowerCase, testMethod, className,
                        targetBackend, excludeDirs, skipIgnored, testRunnerMethodName, additionalRunnerArguments, deep, annotations
                    )
                }
            )
        }
    }
}

fun testGroupSuite(
    args: Array<String>,
    init: TestGroupSuite.() -> Unit
) {
    testGroupSuite(hasDryRunArg(args), init)
}

fun testGroupSuite(
    dryRun: Boolean = false,
    init: TestGroupSuite.() -> Unit
) {
    TestGroupSuite(dryRun).init()
}

class TestGroupSuite(private val dryRun: Boolean) {
    fun testGroup(
        testsRoot: String,
        testDataRoot: String,
        testRunnerMethodName: String = RunTestMethodModel.METHOD_NAME,
        additionalRunnerArguments: List<String> = emptyList(),
        init: TestGroup.() -> Unit
    ) {
        TestGroup(
            testsRoot,
            testDataRoot,
            testRunnerMethodName,
            additionalRunnerArguments,
            dryRun = dryRun
        ).init()
    }
}

interface InconsistencyChecker {
    fun add(affectedFile: String)

    val affectedFiles: List<String>

    companion object {
        fun hasDryRunArg(args: Array<String>) = args.any { it == "dryRun" }

        fun inconsistencyChecker(dryRun: Boolean) = if (dryRun) DefaultInconsistencyChecker else EmptyInconsistencyChecker
    }
}

object DefaultInconsistencyChecker : InconsistencyChecker {
    private val files = mutableListOf<String>()

    override fun add(affectedFile: String) {
        files.add(affectedFile)
    }

    override val affectedFiles: List<String>
        get() = files
}

object EmptyInconsistencyChecker : InconsistencyChecker {
    override fun add(affectedFile: String) {
    }

    override val affectedFiles: List<String>
        get() = emptyList()
}

fun getDefaultSuiteTestClassName(baseTestClassName: String): String {
    require(baseTestClassName.startsWith("Abstract")) { "Doesn't start with \"Abstract\": $baseTestClassName" }
    return baseTestClassName.substringAfter("Abstract") + "Generated"
}
