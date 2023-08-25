/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.generators.model

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.fileNameToJavaIdentifier
import org.jetbrains.kotlin.generators.util.extractTagsFromDirectory
import org.jetbrains.kotlin.generators.util.extractTagsFromTestFile
import org.jetbrains.kotlin.generators.util.methodModelLocator
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import java.util.regex.Pattern

class SimpleTestClassModel(
    val rootFile: File,
    val recursive: Boolean,
    private val excludeParentDirs: Boolean,
    val filenamePattern: Pattern,
    val excludePattern: Pattern?,
    private val checkFilenameStartsLowerCase: Boolean?,
    private val doTestMethodName: String,
    private val testClassName: String,
    val targetBackend: TargetBackend,
    excludeDirs: Collection<String>,
    excludeDirsRecursively: Collection<String>,
    private val skipIgnored: Boolean,
    private val testRunnerMethodName: String,
    private val additionalRunnerArguments: List<String>,
    private val deep: Int?,
    override val annotations: Collection<AnnotationModel>,
    override val tags: List<String>,
    private val additionalMethods: Collection<MethodModel>,
) : TestClassModel() {
    override val name: String
        get() = testClassName

    val excludeDirs: Set<String> = excludeDirs.toSet()
    val excludeDirsRecursively: Set<String> = excludeDirsRecursively.toSet()

    override val innerTestClasses: Collection<TestClassModel> by lazy {
        if (!rootFile.isDirectory || !recursive || deep != null && deep < 1) {
            return@lazy emptyList()
        }
        val children = mutableListOf<TestClassModel>()
        val files = rootFile.listFiles() ?: return@lazy emptyList()
        for (file in files) {
            if (file.isDirectory && dirHasFilesInside(file) && !excludeDirs.contains(file.name) && !excludeDirsRecursively.contains(file.name)) {
                val innerTestClassName = fileNameToJavaIdentifier(file)
                children.add(
                    SimpleTestClassModel(
                        file,
                        true,
                        excludeParentDirs,
                        filenamePattern,
                        excludePattern,
                        checkFilenameStartsLowerCase,
                        doTestMethodName,
                        innerTestClassName,
                        targetBackend,
                        excludesStripOneDirectory(file.name),
                        excludeDirsRecursively,
                        skipIgnored,
                        testRunnerMethodName,
                        additionalRunnerArguments,
                        if (deep != null) deep - 1 else null,
                        annotations,
                        extractTagsFromDirectory(file),
                        additionalMethods.filter { it.shouldBeGeneratedForInnerTestClass() },
                    )
                )
            }
        }
        children.sortWith(BY_NAME)
        children
    }


    private fun excludesStripOneDirectory(directoryName: String): Set<String> {
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

    override val methods: Collection<MethodModel> by lazy {
        if (!rootFile.isDirectory) {
            return@lazy methodModelLocator(
                rootFile,
                rootFile,
                filenamePattern,
                checkFilenameStartsLowerCase,
                targetBackend,
                skipIgnored,
                extractTagsFromTestFile(rootFile)
            )
        }
        val result = mutableListOf<MethodModel>()
        result.add(RunTestMethodModel(targetBackend, doTestMethodName, testRunnerMethodName, additionalRunnerArguments))
        result.add(TestAllFilesPresentMethodModel())
        result.addAll(additionalMethods)
        val listFiles = rootFile.listFiles()
        if (listFiles != null && (deep == null || deep == 0)) {
            for (file in listFiles) {
                val excluded = excludePattern != null && excludePattern.matcher(file.name).matches()
                if (filenamePattern.matcher(file.name).matches() && !excluded) {
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
                    result.addAll(
                        methodModelLocator(
                            rootFile, file, filenamePattern,
                            checkFilenameStartsLowerCase, targetBackend, skipIgnored, extractTagsFromTestFile(file)
                        )
                    )
                }
            }
        }
        if (result.any { it is TransformingTestMethodModel && it.shouldBeGenerated() }) {
            val additionalRunner =
                RunTestMethodModel(targetBackend, doTestMethodName, testRunnerMethodName, additionalRunnerArguments, withTransformer = true)
            result.add(additionalRunner)
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
        get() = KtTestUtil.getFilePath(rootFile)

    override val dataPathRoot: String
        get() = "\$PROJECT_ROOT"

    object TestAllFilesPresentMethodKind : MethodModel.Kind()

    inner class TestAllFilesPresentMethodModel : MethodModel {
        override val kind: MethodModel.Kind
            get() = TestAllFilesPresentMethodKind

        override val name: String
            get() = "testAllFilesPresentIn$testClassName"

        override val dataString: String?
            get() = null

        val classModel: SimpleTestClassModel
            get() = this@SimpleTestClassModel

        override fun shouldBeGenerated(): Boolean {
            return true
        }

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
