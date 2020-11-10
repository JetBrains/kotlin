/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.generators.model

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.*
import java.util.regex.Pattern

class SingleClassTestModel(
    val rootFile: File,
    val filenamePattern: Pattern,
    val excludePattern: Pattern?,
    private val checkFilenameStartsLowerCase: Boolean?,
    private val doTestMethodName: String,
    private val testClassName: String,
    val targetBackend: TargetBackend,
    private val skipIgnored: Boolean,
    private val testRunnerMethodName: String,
    private val additionalRunnerArguments: List<String>,
    override val annotations: List<AnnotationModel>
) : TestClassModel() {
    override val name: String
        get() = testClassName

    override val methods: Collection<MethodModel> by lazy {
        val result: MutableList<MethodModel> = ArrayList()
        result.add(RunTestMethodModel(targetBackend, doTestMethodName, testRunnerMethodName, additionalRunnerArguments))
        result.add(TestAllFilesPresentMethodModel())
        FileUtil.processFilesRecursively(rootFile) { file: File ->
            if (!file.isDirectory && filenamePattern.matcher(file.name).matches()) {
                result.addAll(getTestMethodsFromFile(file))
            }
            true
        }
        result.sortedWith { o1: MethodModel, o2: MethodModel -> o1.name.compareTo(o2.name, ignoreCase = true) }
    }

    override val innerTestClasses: Collection<TestClassModel>
        get() = emptyList()

    private fun getTestMethodsFromFile(file: File): Collection<MethodModel> {
        return listOf(
            SimpleTestMethodModel(
                rootFile, file, filenamePattern, checkFilenameStartsLowerCase, targetBackend, skipIgnored
            )
        )
    }

    // There's always one test for checking if all tests are present
    override val isEmpty: Boolean
        get() = methods.size <= 1
    override val dataString: String = KotlinTestUtils.getFilePath(rootFile)
    override val dataPathRoot: String = "\$PROJECT_ROOT"

    object AllFilesPresentedMethodKind : MethodModel.Kind()

    inner class TestAllFilesPresentMethodModel : MethodModel {
        override val name: String = "testAllFilesPresentIn$testClassName"
        override val dataString: String?
            get() = null

        val classModel: SingleClassTestModel
            get() = this@SingleClassTestModel

        override val kind: MethodModel.Kind
            get() = AllFilesPresentedMethodKind

        override fun shouldBeGenerated(): Boolean {
            return true
        }
    }
}
