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
import org.jetbrains.kotlin.test.services.*

open class MainFunctionForBlackBoxTestsSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    companion object {
        private val PACKAGE_REGEX = """(^|\n)package\s+([\w.]+)""".toRegex()
        private val BOX_METHOD_REGEX = """(^|\n|public\s+)fun\s+box\(\)""".toRegex()
        private val SUSPEND_BOX_METHOD_REGEX = """(^|\n)suspend\s+fun\s+box\(\)""".toRegex()

        const val BOX_MAIN_FILE_NAME = "Generated_Box_Main.kt"

        fun detectPackage(file: TestFile, sourceFileProvider: SourceFileProvider? = null): String? =
            detectPackage(file, sourceContentViewFor(sourceFileProvider), sourceFileProvider)

        fun detectPackage(
            file: TestFile,
            sourceContentView: SourceContentView,
            sourceFileProvider: SourceFileProvider? = null,
        ): String? = PACKAGE_REGEX.find(getSourceContent(file, sourceContentView, sourceFileProvider))?.groups?.get(2)?.value

        fun fileContainsBoxMethod(sourceFile: KtSourceFile): Boolean =
            when (sourceFile) {
                is KtPsiSourceFile -> containsBoxMethod(sourceFile.psiFile.text)
                else -> sourceFile.getContentsAsStream().reader(Charsets.UTF_8).use {
                    containsBoxMethod(it.readText())
                }
            }

        fun containsBoxMethod(fileContent: String): Boolean {
            return BOX_METHOD_REGEX.containsMatchIn(fileContent) || containsSuspendBoxMethod(fileContent)
        }

        private fun containsSuspendBoxMethod(fileContent: String): Boolean {
            return SUSPEND_BOX_METHOD_REGEX.containsMatchIn(fileContent)
        }

        fun containsSuspendBoxMethod(file: TestFile, sourceFileProvider: SourceFileProvider? = null): Boolean =
            containsSuspendBoxMethod(file, sourceContentViewFor(sourceFileProvider), sourceFileProvider)

        fun containsSuspendBoxMethod(
            file: TestFile,
            sourceContentView: SourceContentView,
            sourceFileProvider: SourceFileProvider? = null,
        ): Boolean = containsSuspendBoxMethod(getSourceContent(file, sourceContentView, sourceFileProvider))

        fun containsBoxMethod(file: TestFile, sourceFileProvider: SourceFileProvider? = null): Boolean =
            containsBoxMethod(file, sourceContentViewFor(sourceFileProvider), sourceFileProvider)

        fun containsBoxMethod(
            file: TestFile,
            sourceContentView: SourceContentView,
            sourceFileProvider: SourceFileProvider? = null,
        ): Boolean = containsBoxMethod(getSourceContent(file, sourceContentView, sourceFileProvider))

        fun containsBoxMethod(module: TestModule, sourceFileProvider: SourceFileProvider? = null): Boolean =
            containsBoxMethod(module, sourceContentViewFor(sourceFileProvider), sourceFileProvider)

        fun containsBoxMethod(
            module: TestModule,
            sourceContentView: SourceContentView,
            sourceFileProvider: SourceFileProvider? = null,
        ): Boolean = module.files.any { containsBoxMethod(it, sourceContentView, sourceFileProvider) }

        fun containsBoxMethod(moduleStructure: TestModuleStructure, sourceFileProvider: SourceFileProvider? = null): Boolean =
            containsBoxMethod(moduleStructure, sourceContentViewFor(sourceFileProvider), sourceFileProvider)

        fun containsBoxMethod(
            moduleStructure: TestModuleStructure,
            sourceContentView: SourceContentView,
            sourceFileProvider: SourceFileProvider? = null,
        ): Boolean = moduleStructure.modules.any { containsBoxMethod(it, sourceContentView, sourceFileProvider) }

        fun containsBoxMethod(testServices: TestServices): Boolean {
            val sourceFileProvider = testServices.sourceFileProviderOrNull
            return containsBoxMethod(
                testServices.moduleStructure,
                sourceContentViewFor(sourceFileProvider),
                sourceFileProvider,
            )
        }

        fun findFileWithBoxMethod(module: TestModule, sourceFileProvider: SourceFileProvider? = null): TestFile? =
            findFileWithBoxMethod(module, sourceContentViewFor(sourceFileProvider), sourceFileProvider)

        fun findFileWithBoxMethod(
            module: TestModule,
            sourceContentView: SourceContentView,
            sourceFileProvider: SourceFileProvider? = null,
        ): TestFile? = module.files.firstOrNull { containsBoxMethod(it, sourceContentView, sourceFileProvider) }

        fun findFileWithBoxMethod(modules: Iterable<TestModule>, sourceFileProvider: SourceFileProvider? = null): TestFile? =
            findFileWithBoxMethod(modules, sourceContentViewFor(sourceFileProvider), sourceFileProvider)

        fun findFileWithBoxMethod(
            modules: Iterable<TestModule>,
            sourceContentView: SourceContentView,
            sourceFileProvider: SourceFileProvider? = null,
        ): TestFile? = modules.firstNotNullOfOrNull { findFileWithBoxMethod(it, sourceContentView, sourceFileProvider) }

        fun findFileWithBoxMethod(moduleStructure: TestModuleStructure, sourceFileProvider: SourceFileProvider? = null): TestFile? =
            findFileWithBoxMethod(moduleStructure, sourceContentViewFor(sourceFileProvider), sourceFileProvider)

        fun findFileWithBoxMethod(
            moduleStructure: TestModuleStructure,
            sourceContentView: SourceContentView,
            sourceFileProvider: SourceFileProvider? = null,
        ): TestFile? = findFileWithBoxMethod(moduleStructure.modules, sourceContentView, sourceFileProvider)

        fun findFileWithBoxMethod(testServices: TestServices): TestFile? {
            val sourceFileProvider = testServices.sourceFileProviderOrNull
            return findFileWithBoxMethod(
                testServices.moduleStructure,
                sourceContentViewFor(sourceFileProvider),
                sourceFileProvider,
            )
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

        // ModuleStructureExtractor invokes additional source providers before it registers the structure in
        // TestServices. Inspect the original content here instead of asking SourceFileProvider to resolve it through
        // the not-yet-registered structure.
        val fileWithBox = findFileWithBoxMethod(module, SourceContentView.ORIGINAL) ?: return emptyList()
        val suspendModifier = if (containsSuspendBoxMethod(fileWithBox, SourceContentView.ORIGINAL)) "suspend " else ""
        val mainBody = generateMainBody()

        val code = buildString {
            detectPackage(fileWithBox, SourceContentView.ORIGINAL)?.let {
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
