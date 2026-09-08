/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.dsl

import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.*
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.extractTagsFromDirectory
import java.io.File
import java.util.regex.Pattern

fun TestGroupSuite.forEachTestClassParallel(f: (TestGroup.TestClass) -> Unit) {
    testGroups
        .parallelStream()
        .flatMap { it.testClasses.stream() }
        .sorted(compareByDescending { it.testModels.sumOf { it.methods.size } })
        .forEach(f)
}

class TestGroupSuite(val defaultSkipTestAllFilesCheck: Boolean) {
    val testGroups: List<TestGroup>
        field = mutableListOf<TestGroup>()

    fun testGroup(
        testsRoot: String,
        testDataRoot: String,
        testRunnerMethodName: String = MethodGenerator.DEFAULT_RUN_TEST_METHOD_NAME,
        init: TestGroup.() -> Unit,
    ) {
        testGroups += TestGroup(
            testsRoot,
            testDataRoot,
            testRunnerMethodName,
            defaultSkipTestAllFilesCheck,
        ).apply(init)
    }
}

class TestGroup(
    private val testsRoot: String,
    val testDataRoot: String,
    val testRunnerMethodName: String,
    val defaultSkipTestAllFilesCheck: Boolean,
) {
    val testClasses: List<TestClass>
        field: MutableList<TestClass> = mutableListOf()

    inline fun <reified T> testClass(
        suiteTestClassName: String = getDefaultSuiteTestClassName(T::class.java.simpleName),
        annotations: List<AnnotationModel> = emptyList(),
        noinline init: TestClass.() -> Unit,
    ) {
        val testKClass = T::class.java
        testClass(testKClass, testKClass.name, suiteTestClassName, annotations, init)
    }

    fun testClass(
        testKClass: Class<*>,
        baseTestClassName: String = testKClass.name,
        suiteTestClassName: String = getDefaultSuiteTestClassName(baseTestClassName.substringAfterLast('.')),
        annotations: List<AnnotationModel> = emptyList(),
        init: TestClass.() -> Unit,
    ) {
        testClasses += TestClass(testKClass, baseTestClassName, suiteTestClassName, annotations).apply(init)
    }

    inner class TestClass(
        val testKClass: Class<*>,
        val baseTestClassName: String,
        val suiteTestClassName: String,
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
            excludedPattern: String? = null,
        ) {
            model(
                "${relativePath}/${testDirectoryName}",
                extension = extension,
                recursive = recursive,
                excludeParentDirs = excludeParentDirs,
                excludedPattern = excludedPattern,
                testClassName = testDirectoryName.replaceFirstChar { it.uppercaseChar() } + testKClass.simpleName,
            )
        }

        /**
         * Declares a class model for testdata in [relativeRootPath] (relative to outer [TestGroup.testDataRoot]).
         *
         * @param extension defines extension for files which would be considered as testdata (null string means dir (name without dot))
         * @param testClassName defines specific name for generated test class
         * @param targetBackend defines the backend of a specific test. Later it would be used by the test
         *   suppression machinery if there are some `IGNORE_BACKEND` directives in the test.
         *   Note that it's necessary only for legacy JUnit3/4 tests; for JUnit5 tests this parameter is obsolete and should be avoided.
         */
        fun model(
            relativeRootPath: String = "",
            recursive: Boolean = true,
            excludeParentDirs: Boolean = false,
            extension: String? = "kt",
            pattern: String = if (extension == null) """^([^.]+)$""" else """^(.+)\.$extension$""",
            excludedPattern: String? = null,
            testMethod: String = "doTest",
            testClassName: String? = null,
            excludeDirs: List<String> = listOf(),
            excludeDirsRecursively: List<String> = listOf(),
            skipTestAllFilesCheck: Boolean = defaultSkipTestAllFilesCheck,
            smokeTest: Boolean = false,
            smokeTestLimit: Int = 1,
        ) {
            val rootFile = File("$testDataRoot/$relativeRootPath")
            val compiledPattern = Pattern.compile(pattern)
            val compiledExcludedPattern = excludedPattern?.let { Pattern.compile(it) }
            val className = testClassName ?: TestGeneratorUtil.fileNameToJavaIdentifier(rootFile)

            testModels.add(
                SimpleTestClassModel(
                    File(testDataRoot), rootFile, recursive, excludeParentDirs, compiledPattern,
                    compiledExcludedPattern, testMethod, className, excludeDirs,
                    excludeDirsRecursively, testRunnerMethodName, annotations, extractTagsFromDirectory(rootFile), methodModels,
                    skipTestAllFilesCheck, testKClass, isSmokeTest = smokeTest, smokeTestLimit = smokeTestLimit
                )
            )
        }
    }
}

fun getDefaultSuiteTestClassName(baseTestClassName: String): String {
    require(baseTestClassName.startsWith("Abstract")) { "Doesn't start with \"Abstract\": $baseTestClassName" }
    return baseTestClassName.substringAfter("Abstract") + "Generated"
}

/**
 * Declares one generated test class per testdata directory under [relativeRootPath], as opposed to a
 * single [TestGroup.TestClass.model] call with `recursive = true`, which produces one class with a
 * tree of `@Nested` classes for the whole directory tree.
 *
 * Each generated class covers only the testdata files lying directly in its own directory; every
 * nested directory gets a class of its own. The class name is the one a plain [TestGroup.testClass]
 * call would produce, and the directory structure is mapped onto the package name:
 *
 * ```
 * directory  compiler/testData/diagnostics/tests/inference/pcla
 * class      <generatedPackage>.tests.inference.pcla.PhasedJvmDiagnosticLightTreeTestGenerated
 * ```
 *
 * Many small classes instead of one huge one keep test generation and test compilation incremental,
 * and let the build declare a separate test task with per-directory inputs for each of them.
 *
 * @param generatedPackage package to put the generated classes into. The name of the
 *   [relativeRootPath] directory and the path of each nested directory are appended to it, so
 *   several roots may share a single [generatedPackage].
 */
fun TestGroup.testClassPerDirectory(
    testKClass: Class<*>,
    relativeRootPath: String,
    generatedPackage: String,
    annotations: List<AnnotationModel> = emptyList(),
    extension: String? = "kt",
    pattern: String = if (extension == null) """^([^.]+)$""" else """^(.+)\.$extension$""",
    excludedPattern: String? = null,
    testMethod: String = "doTest",
    excludeDirs: List<String> = listOf(),
    excludeDirsRecursively: List<String> = listOf(),
    skipTestAllFilesCheck: Boolean = defaultSkipTestAllFilesCheck,
) {
    val testDataRootFile = File(testDataRoot)
    val rootFile = File(testDataRootFile, relativeRootPath)
    require(rootFile.isDirectory) { "Testdata root is not a directory: $rootFile" }

    val suiteTestClassName = getDefaultSuiteTestClassName(testKClass.simpleName)
    val rootPackage = "$generatedPackage.${TestGeneratorUtil.directoryNameToPackageSegment(rootFile.name)}"

    forEachTestDataDirectory(
        rootFile,
        filenamePattern = Pattern.compile(pattern),
        excludePattern = excludedPattern?.let { Pattern.compile(it) },
        excludeDirs = excludeDirs,
        excludeDirsRecursively = excludeDirsRecursively,
    ) { directory, packagePath, excludeDirsForDirectory ->
        testClass(
            testKClass,
            suiteTestClassName = (listOf(rootPackage) + packagePath + suiteTestClassName).joinToString("."),
            annotations = annotations,
        ) {
            model(
                relativeRootPath = directory.toRelativeString(testDataRootFile).replace(File.separatorChar, '/'),
                recursive = false,
                extension = extension,
                pattern = pattern,
                excludedPattern = excludedPattern,
                testMethod = testMethod,
                excludeDirs = excludeDirsForDirectory.toList(),
                excludeDirsRecursively = excludeDirsRecursively,
                skipTestAllFilesCheck = skipTestAllFilesCheck,
            )
        }
    }
}

/**
 * Traverses the testdata tree starting at [root] and invokes [consume] for every directory which
 * needs a generated test class of its own, i.e. which has testdata lying directly inside it.
 *
 * [consume] receives the directory, the package path accumulated for it (relative to [root], which
 * itself gets an empty one), and [excludeDirs] reinterpreted for that directory.
 *
 * The traversal repeats the filtering rules of [SimpleTestClassModel] so that the union of the
 * generated classes covers exactly the same testdata files as a single recursive model would.
 */
private fun forEachTestDataDirectory(
    root: File,
    filenamePattern: Pattern,
    excludePattern: Pattern?,
    excludeDirs: Collection<String>,
    excludeDirsRecursively: Collection<String>,
    consume: (directory: File, packagePath: List<String>, excludeDirs: Collection<String>) -> Unit,
) {
    fun File.isTestData(): Boolean =
        filenamePattern.matcher(name).matches() && excludePattern?.matcher(name)?.matches() != true

    fun visit(directory: File, packagePath: List<String>, excludeDirs: Collection<String>) {
        val allExcludedDirs = excludeDirs.toSet() + excludeDirsRecursively
        val children = directory.listFiles().orEmpty()

        // Only entries lying directly in the directory become test methods of its class, so a
        // directory without them doesn't need a class at all (an empty one wouldn't even be generated).
        if (children.any { it.isTestData() && !(it.isDirectory && it.name in allExcludedDirs) }) {
            consume(directory, packagePath, excludeDirs)
        }

        for (child in children.sortedBy { it.name }) {
            if (!child.isDirectory || child.name in allExcludedDirs) continue
            // A directory which is a test case on its own (`extension = null`) is a method, not a class
            if (child.isTestData()) continue
            visit(
                child,
                packagePath + TestGeneratorUtil.directoryNameToPackageSegment(child.name),
                excludesStripOneDirectory(excludeDirs, child.name),
            )
        }
    }

    visit(root, emptyList(), excludeDirs)
}
