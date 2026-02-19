/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.generators.model

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.generators.model.methods.RunTestMethodModel
import org.jetbrains.kotlin.generators.model.methods.RunTestWithDirectoryPrefixMethodModel
import org.jetbrains.kotlin.generators.model.methods.SimpleTestMethodModel
import org.jetbrains.kotlin.generators.model.methods.TestAllFilesPresentMethodModel
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.fileNameToJavaIdentifier
import org.jetbrains.kotlin.generators.util.extractTagsFromDirectory
import org.jetbrains.kotlin.generators.util.extractTagsFromTestFile
import org.jetbrains.kotlin.generators.util.getFilePath
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.regex.Pattern

/**
 * The main implementation of a class model.
 * Class structure reflects the testdata directory structure, where
 * - directory is a class
 * - subdirectory is a nested/inner class (for JUnit4/JUnit5 respectively)
 * - a test data file is a test method
 *
 * The model encapsulates the logic of traversing the testdata directory,
 * locating testdata files and generating test method models for them.
 *
 * @property recursive if false then subdirectories wouldn't be traversed
 */
class SimpleTestClassModel(
    val testInfraRevision: TestInfraRevision,
    val rootFile: File,
    val recursive: Boolean,
    private val excludeParentDirs: Boolean,
    val filenamePattern: Pattern,
    val excludePattern: Pattern?,
    private val doTestMethodName: String,
    val testClassName: String,
    val targetBackend: TargetBackend?,
    excludeDirs: Collection<String>,
    excludeDirsRecursively: Collection<String>,
    private val testRunnerMethodName: String,
    override val annotations: Collection<AnnotationModel>,
    override val tags: List<String>,
    private val additionalMethods: Collection<MethodModel<*>>,
    val skipTestAllFilesCheck: Boolean,
) : TestClassModel() {
    override val name: String
        get() = testClassName

    val allExcludedDirs: Set<String> = (excludeDirs + excludeDirsRecursively).toSet()

    override val innerTestClasses: Collection<TestClassModel> by lazy {
        if (!rootFile.isDirectory || !recursive) {
            return@lazy emptyList()
        }
        rootFile.listFiles().orEmpty().mapNotNull l@{ file ->
            if (!file.isDirectory) return@l null
            if (!dirHasFilesInside(file)) return@l null
            if (allExcludedDirs.contains(file.name)) return@l null

            SimpleTestClassModel(
                testInfraRevision,
                rootFile = file,
                recursive = true,
                excludeParentDirs,
                filenamePattern,
                excludePattern,
                doTestMethodName,
                testClassName = fileNameToJavaIdentifier(file),
                targetBackend,
                excludesStripOneDirectory(excludeDirs, file.name),
                excludeDirsRecursively,
                testRunnerMethodName,
                annotations,
                extractTagsFromDirectory(file),
                additionalMethods.filter { it.shouldBeGeneratedForInnerTestClass },
                skipTestAllFilesCheck,
            )
        }.sortedWith(BY_NAME)
    }

    private fun excludesStripOneDirectory(excludeDirs: Collection<String>, directoryName: String): Collection<String> {
        if (excludeDirs.isEmpty()) return excludeDirs
        val result: MutableSet<String> = LinkedHashSet()
        for (excludeDir in excludeDirs) {
            val firstSlash = excludeDir.indexOf('/')
            if (firstSlash >= 0 && excludeDir.substring(0, firstSlash) == directoryName) {
                result.add(excludeDir.substring(firstSlash + 1))
            }
        }
        return result
    }

    override val methods: Collection<MethodModel<*>> by lazy {
        if (!rootFile.isDirectory) {
            val methodModel = SimpleTestMethodModel(
                testInfraRevision,
                rootDir = rootFile,
                file = rootFile,
                filenamePattern,
                extractTagsFromTestFile(rootFile)
            )
            return@lazy listOf(methodModel)
        }

        buildList {
            when (testInfraRevision) {
                TestInfraRevision.LegacyJUnit4 -> add(RunTestMethodModel(targetBackend, doTestMethodName, testRunnerMethodName))
                TestInfraRevision.StandardJUnit5 -> add(RunTestWithDirectoryPrefixMethodModel(rootFile.getFilePath()))
            }
            if (!skipTestAllFilesCheck) {
                add(TestAllFilesPresentMethodModel(this@SimpleTestClassModel))
            }
            addAll(additionalMethods)
            rootFile.listFiles().orEmpty().mapNotNullTo(this) l@{ file ->
                val fileName = file.name
                // doesn't match testdata pattern
                if (!filenamePattern.matcher(fileName).matches()) return@l null

                // excluded by pattern
                if (excludePattern != null && excludePattern.matcher(fileName).matches()) return@l null

                // excluded by directory
                if (file.isDirectory && (fileName in allExcludedDirs)) return@l null

                if (file.isDirectory && excludeParentDirs && dirHasSubDirs(file)) return@l null

                if (file.isDirectory && !dirHasFilesInside(file)) {
                    error(
                        "testData directory $file is empty. " +
                                "This might be due to git branch switching removed the contents but left directory intact. " +
                                "Consider removing empty directory or revert removing of its' contents."
                    )
                }
                SimpleTestMethodModel(
                    testInfraRevision,
                    rootFile,
                    file,
                    filenamePattern,
                    extractTagsFromTestFile(file)
                )
            }
        }.sortedWith(BY_NAME)
    }

    override val isEmpty: Boolean
        get() {
            val noTestMethods = methods.size == 1
            return noTestMethods && innerTestClasses.isEmpty()
        }

    override val dataString: String
        get() = rootFile.getFilePath()

    override val dataPathRoot: String
        get() = "\$PROJECT_ROOT"

    companion object {
        private val BY_NAME = Comparator.comparing(TestEntityModel::name)

        private fun dirHasFilesInside(dir: File): Boolean {
            return !FileUtil.processFilesRecursively(dir) { obj: File -> obj.isDirectory }
        }

        private fun dirHasSubDirs(dir: File): Boolean {
            val listFiles = dir.listFiles() ?: return false
            for (file in listFiles) {
                if (file.isDirectory) {
                    return true
                }
            }
            return false
        }
    }
}
