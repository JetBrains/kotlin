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

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.utils.Jsr305State
import java.io.File

val FOREIGN_ANNOTATIONS_SOURCES_PATH = "third-party/annotations"
val TEST_ANNOTATIONS_SOURCE_PATH = "compiler/testData/foreignAnnotations/testAnnotations"

abstract class AbstractForeignAnnotationsTest : AbstractDiagnosticsTest() {
    private val WARNING_FOR_JSR305_ANNOTATIONS_DIRECTIVE = "WARNING_FOR_JSR305_ANNOTATIONS"
    private val JSR305_ANNOTATIONS_IGNORE_DIRECTIVE = "JSR305_ANNOTATIONS_IGNORE"

    override fun getExtraClasspath(): List<File> {
        val foreignAnnotations = listOf(MockLibraryUtil.compileJvmLibraryToJar(annotationsPath, "foreign-annotations"))
        return foreignAnnotations + compileTestAnnotations(foreignAnnotations)
    }

    protected fun compileTestAnnotations(extraClassPath: List<File>): List<File> =
            listOf(MockLibraryUtil.compileJvmLibraryToJar(
                TEST_ANNOTATIONS_SOURCE_PATH,
                "test-foreign-annotations",
                extraClasspath = extraClassPath.map { it.path }
        ))

    override fun getConfigurationKind(): ConfigurationKind = ConfigurationKind.ALL

    override fun getTestJdkKind(file: File): TestJdkKind = TestJdkKind.FULL_JDK

    open protected val annotationsPath: String
        get() = FOREIGN_ANNOTATIONS_SOURCES_PATH

    override fun loadLanguageVersionSettings(module: List<TestFile>): LanguageVersionSettings {
        val hasWarningDirective = module.any {
            InTextDirectivesUtils.isDirectiveDefined(it.expectedText, WARNING_FOR_JSR305_ANNOTATIONS_DIRECTIVE)
        }

        val hasIgnoreDirective = module.any {
            InTextDirectivesUtils.isDirectiveDefined(it.expectedText, JSR305_ANNOTATIONS_IGNORE_DIRECTIVE)
        }

        val jsr305State = when {
            hasIgnoreDirective -> Jsr305State.IGNORE
            hasWarningDirective -> Jsr305State.WARN
            else -> Jsr305State.STRICT
        }

        return LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE,
                                           mapOf(AnalysisFlag.jsr305 to jsr305State)
        )
    }
}
