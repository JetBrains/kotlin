/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.sourceProviders

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.REQUIRES_SEPARATE_PROCESS
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager

open class MainFunctionForBlackBoxTestsSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    companion object {
        private val PACKAGE_REGEX = """(^|\n)package ([\w.]+)""".toRegex()
        private val BOX_METHOD_REGEX = """(^|\n)fun box\(\)""".toRegex()
        private val SUSPEND_BOX_METHOD_REGEX = """(^|\n)suspend fun box\(\)""".toRegex()

        const val BOX_MAIN_FILE_NAME = "Generated_Box_Main.kt"

        fun detectPackage(file: TestFile): String? {
            return PACKAGE_REGEX.find(file.originalContent)?.groups?.get(2)?.value
        }

        fun fileContainsBoxMethod(sourceFile: KtSourceFile): Boolean =
            when (sourceFile) {
                is KtPsiSourceFile -> containsBoxMethod(sourceFile.psiFile.text)
                else -> with(sourceFile.getContentsAsStream().reader(Charsets.UTF_8)) {
                    containsBoxMethod(this.readText())
                }
            }

        private fun containsBoxMethod(fileContent: String): Boolean {
            return BOX_METHOD_REGEX.containsMatchIn(fileContent) || containsSuspendBoxMethod(fileContent)
        }

        private fun containsSuspendBoxMethod(fileContent: String): Boolean {
            return SUSPEND_BOX_METHOD_REGEX.containsMatchIn(fileContent)
        }
    }

    protected open fun generateMainBody(): String {
        return """
            val res = box()
            if (res != "OK") throw AssertionError(res)
        """.trimIndent()
    }

    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        if (REQUIRES_SEPARATE_PROCESS !in module.directives && module.directives.singleOrZeroValue(JDK_KIND)?.requiresSeparateProcess != true) {
            return emptyList()
        }

        val fileWithBox = module.files.firstOrNull { containsBoxMethod(it.originalContent) } ?: return emptyList()
        val suspendModifier = if (containsSuspendBoxMethod(fileWithBox.originalContent)) "suspend " else ""
        val mainBody = generateMainBody()

        val code = buildString {
            detectPackage(fileWithBox)?.let {
                appendLine("package $it")
            }
            appendLine(
                """
                    ${suspendModifier}fun main() {
                        $mainBody
                    }
                """.trimIndent()
            )
        }
        val file = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("src").resolve(BOX_MAIN_FILE_NAME)
        file.writeText(code)

        return listOf(file.toTestFile())
    }
}
