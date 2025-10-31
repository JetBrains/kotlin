/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.generators.model

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.impl.SimpleTestClassModelTestAllFilesPresentMethodGenerator
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
    val rootFile: File,
    val recursive: Boolean,
    private val excludeParentDirs: Boolean,
    val filenamePattern: Pattern,
    val excludePattern: Pattern?,
    private val doTestMethodName: String,
    private val testClassName: String,
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
        val children = mutableListOf<TestClassModel>()
        val files = rootFile.listFiles() ?: return@lazy emptyList()
        for (file in files) {
            if (file.isDirectory && dirHasFilesInside(file) && !allExcludedDirs.contains(file.name)) {
                val innerTestClassName = fileNameToJavaIdentifier(file)
                children.add(
                    SimpleTestClassModel(
                        file,
                        true,
                        excludeParentDirs,
                        filenamePattern,
                        excludePattern,
                        doTestMethodName,
                        innerTestClassName,
                        targetBackend,
                        excludesStripOneDirectory(excludeDirs, file.name),
                        excludeDirsRecursively,
                        testRunnerMethodName,
                        annotations,
                        extractTagsFromDirectory(file),
                        additionalMethods.filter { it.shouldBeGeneratedForInnerTestClass },
                        skipTestAllFilesCheck,
                    )
                )
            }
        }
        children.sortWith(BY_NAME)
        children
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
                rootDir = rootFile,
                file = rootFile,
                filenamePattern,
                extractTagsFromTestFile(rootFile)
            )
            return@lazy listOf(methodModel)
        }
        val result = mutableListOf<MethodModel<*>>()
        result.add(RunTestMethodModel(targetBackend, doTestMethodName, testRunnerMethodName))
        if (!skipTestAllFilesCheck) {
            result.add(TestAllFilesPresentMethodModel())
        }
        result.addAll(additionalMethods)
        val listFiles = rootFile.listFiles()
        if (listFiles != null) {
            for (file in listFiles) {
                val excluded = let {
                    val name = file.name
                    val byPattern = excludePattern != null && excludePattern.matcher(name).matches()
                    val byDirectory = file.isDirectory && (name in allExcludedDirs)
                    return@let byPattern || byDirectory
                }
                if (!excluded && filenamePattern.matcher(file.name).matches()) {
                    if (file.isDirectory && excludeParentDirs && dirHasSubDirs(file)) {
                        continue
                    }
                    if (file.isDirectory && !dirHasFilesInside(file)) {
                        throw IllegalStateException(
                            "testData directory $file is empty. " +
                                    "This might be due to git branch switching removed the contents but left directory intact. " +
                                    "Consider removing empty directory or revert removing of its' contents."
                        )
                    }
                    result.add(
                        SimpleTestMethodModel(
                            rootFile,
                            file,
                            filenamePattern,
                            extractTagsFromTestFile(file)
                        )
                    )
                }
            }
        }
        result.sortWith(BY_NAME)
        result
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

    /**
     * Test method which ensures that there is a generated test for each testdata file in the
     *   corresponding directory. Used to validate that generated test is up-to-date.
     */
    inner class TestAllFilesPresentMethodModel : MethodModel<TestAllFilesPresentMethodModel>() {
        override val generator: MethodGenerator<TestAllFilesPresentMethodModel>
            get() = SimpleTestClassModelTestAllFilesPresentMethodGenerator

        override val name: String
            get() = "testAllFilesPresentIn$testClassName"

        override val dataString: String?
            get() = null

        val classModel: SimpleTestClassModel
            get() = this@SimpleTestClassModel

        override val tags: List<String>
            get() = emptyList()
    }

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
