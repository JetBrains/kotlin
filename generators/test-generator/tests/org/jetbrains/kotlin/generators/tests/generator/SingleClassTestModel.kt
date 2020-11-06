/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.generators.tests.generator

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.Printer
import java.io.File
import java.util.*
import java.util.regex.Pattern

class SingleClassTestModel(
    private val rootFile: File,
    private val filenamePattern: Pattern,
    private val excludePattern: Pattern?,
    private val checkFilenameStartsLowerCase: Boolean?,
    private val doTestMethodName: String,
    private val testClassName: String,
    private val targetBackend: TargetBackend,
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

    private fun getTestMethodsFromFile(file: File): Collection<TestMethodModel> {
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

    private inner class TestAllFilesPresentMethodModel : TestMethodModel() {
        override val name: String = "testAllFilesPresentIn$testClassName"
        override val dataString: String?
            get() = null

        override fun generateBody(p: Printer) {
            val assertTestsPresentStr: String
            val excludedArgument = if (excludePattern != null) {
                String.format(
                    "Pattern.compile(\"%s\")", StringUtil.escapeStringCharacters(
                        excludePattern.pattern()
                    )
                )
            } else {
                null
            }
            assertTestsPresentStr = if (targetBackend !== TargetBackend.ANY) {
                String.format(
                    "KotlinTestUtils.assertAllTestsPresentInSingleGeneratedClassWithExcluded(this.getClass(), new File(\"%s\"), Pattern.compile(\"%s\"), %s, %s.%s);",
                    KotlinTestUtils.getFilePath(rootFile), StringUtil.escapeStringCharacters(filenamePattern.pattern()),
                    excludedArgument, TargetBackend::class.java.simpleName, targetBackend.toString()
                )
            } else {
                String.format(
                    "KotlinTestUtils.assertAllTestsPresentInSingleGeneratedClassWithExcluded(this.getClass(), new File(\"%s\"), Pattern.compile(\"%s\"), %s);",
                    KotlinTestUtils.getFilePath(rootFile), StringUtil.escapeStringCharacters(filenamePattern.pattern()), excludedArgument
                )
            }
            p.println(assertTestsPresentStr)
        }

        override fun shouldBeGenerated(): Boolean {
            return true
        }
    }
}
