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

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.utils.Jsr305State
import org.jetbrains.kotlin.utils.ReportLevel
import java.io.File

val FOREIGN_ANNOTATIONS_SOURCES_PATH = "third-party/annotations"
val TEST_ANNOTATIONS_SOURCE_PATH = "compiler/testData/foreignAnnotations/testAnnotations"

abstract class AbstractForeignAnnotationsTest : AbstractDiagnosticsTest() {
    private val JSR305_GLOBAL_DIRECTIVE = "JSR305_GLOBAL_REPORT"
    private val JSR305_MIGRATION_DIRECTIVE = "JSR305_MIGRATION_REPORT"
    private val JSR305_SPECIAL_DIRECTIVE = "JSR305_SPECIAL_REPORT"

    override fun getExtraClasspath(): List<File> {
        val foreignAnnotations = createJarWithForeignAnnotations()
        return foreignAnnotations + compileTestAnnotations(foreignAnnotations)
    }

    protected fun compileTestAnnotations(extraClassPath: List<File>): List<File> =
            listOf(MockLibraryUtil.compileJavaFilesLibraryToJar(
                TEST_ANNOTATIONS_SOURCE_PATH,
                "test-foreign-annotations",
                extraOptions = listOf("-Xallow-kotlin-package"),
                extraClasspath = extraClassPath.map { it.path }
        ))

    protected fun createJarWithForeignAnnotations(): List<File> = listOf(
            MockLibraryUtil.compileJavaFilesLibraryToJar(annotationsPath, "foreign-annotations"),
            ForTestCompileRuntime.jvmAnnotationsForTests()
    )

    override fun getConfigurationKind(): ConfigurationKind = ConfigurationKind.ALL

    override fun getTestJdkKind(file: File): TestJdkKind = TestJdkKind.FULL_JDK

    open protected val annotationsPath: String
        get() = FOREIGN_ANNOTATIONS_SOURCES_PATH

    override fun loadLanguageVersionSettings(module: List<TestFile>): LanguageVersionSettings {
        val analysisFlags = loadAnalysisFlags(module)
        return LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE, analysisFlags)
    }

    private fun loadAnalysisFlags(module: List<TestFile>): Map<AnalysisFlag<*>, Any?> {
        val globalState = module.getDirectiveValue(JSR305_GLOBAL_DIRECTIVE) ?: ReportLevel.STRICT
        val migrationState = module.getDirectiveValue(JSR305_MIGRATION_DIRECTIVE)

        val userAnnotationsState = module.flatMap {
            InTextDirectivesUtils.findListWithPrefixes(it.expectedText, JSR305_SPECIAL_DIRECTIVE)
        }.mapNotNull {
            val (name, stateDescription) = it.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
            val state = ReportLevel.findByDescription(stateDescription) ?: return@mapNotNull null

            name to state
        }.toMap()

        return mapOf(AnalysisFlag.jsr305 to Jsr305State(globalState, migrationState, userAnnotationsState))
    }

    private fun List<TestFile>.getDirectiveValue(directive: String): ReportLevel? = mapNotNull {
            InTextDirectivesUtils.findLinesWithPrefixesRemoved(it.expectedText, directive).firstOrNull()
    }.firstOrNull().let { ReportLevel.findByDescription(it) }
}
