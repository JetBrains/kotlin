/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.sourceProviders

import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.REQUIRES_SEPARATE_PROCESS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager

class MainFunctionForBlackBoxTestsSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    companion object {
        private val PACKAGE_REGEXP = """package ([\w.]+)""".toRegex()
        private val START_BOX_METHOD_REGEX = """^fun box\(\)""".toRegex()
        private val MIDDLE_BOX_METHOD_REGEX = """\nfun box\(\)""".toRegex()

        const val BOX_MAIN_FILE_NAME = "Generated_Box_Main.kt"

        fun detectPackage(file: TestFile): String? {
            return PACKAGE_REGEXP.find(file.originalContent)?.groups?.get(1)?.value
        }

        fun containsBoxMethod(file: TestFile): Boolean {
            return containsBoxMethod(file.originalContent)
        }

        fun containsBoxMethod(fileContent: String): Boolean {
            return START_BOX_METHOD_REGEX.containsMatchIn(fileContent) || MIDDLE_BOX_METHOD_REGEX.containsMatchIn(fileContent)
        }
    }

    override fun produceAdditionalFiles(globalDirectives: RegisteredDirectives, module: TestModule): List<TestFile> {
        if (REQUIRES_SEPARATE_PROCESS !in module.directives && module.directives.singleOrZeroValue(JDK_KIND)?.requiresSeparateProcess != true) {
            return emptyList()
        }

        val fileWithBox = module.files.firstOrNull { containsBoxMethod(it) } ?: return emptyList()

        val code = buildString {
            detectPackage(fileWithBox)?.let {
                appendLine("package $it")
            }
            appendLine(
                """
                    fun main() {
                        val res = box()
                        if (res != "OK") throw AssertionError(res)
                    }
                """.trimIndent()
            )
        }
        val file = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("src").resolve(BOX_MAIN_FILE_NAME)
        file.writeText(code)

        return listOf(file.toTestFile())
    }
}
