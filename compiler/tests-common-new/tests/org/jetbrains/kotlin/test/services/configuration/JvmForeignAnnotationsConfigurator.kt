/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.JSPECIFY_STATE
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.JSR305_GLOBAL_REPORT
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.JSR305_MIGRATION_REPORT
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.JSR305_SPECIAL_REPORT
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.JavaTypeEnhancementState
import org.jetbrains.kotlin.utils.ReportLevel
import java.io.File

enum class JdkForeignAnnotationType(val path: String) {
    Annotations("third-party/annotations"),
    Jdk8Annotations("third-party/jdk8-annotations");

    companion object {
        val FOREIGN_ANNOTATIONS_SOURCES_PATH = Annotations.path
        val FOREIGN_JDK8_ANNOTATIONS_SOURCES_PATH = Jdk8Annotations.path
    }
}

open class JvmForeignAnnotationsConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        private const val TEST_ANNOTATIONS_SOURCE_PATH = "compiler/testData/foreignAnnotations/testAnnotations"
    }

    override val directivesContainers: List<DirectivesContainer>
        get() = listOf(ForeignAnnotationsDirectives)

    @OptIn(ExperimentalStdlibApi::class)
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val extraClassPath = buildList {
            val foreignAnnotations = createJarWithForeignAnnotations(module)
            addAll(foreignAnnotations)
            addAll(compileTestAnnotations(foreignAnnotations))
        }
        configuration.addJvmClasspathRoots(extraClassPath)
    }

    protected fun createJarWithForeignAnnotations(module: TestModule): List<File> {
        val directive = ForeignAnnotationsDirectives.ANNOTATIONS_PATH
        val annotationsPath = module.directives.singleOrZeroValue(directive)
            ?.path
            ?: error("${directive.name} should be specified in test or test runner")

        return listOf(
            MockLibraryUtil.compileJavaFilesLibraryToJar(annotationsPath, "foreign-annotations", assertions = JUnit5Assertions),
            ForTestCompileRuntime.jvmAnnotationsForTests()
        )
    }

    protected fun compileTestAnnotations(extraClassPath: List<File>): List<File> =
        listOf(
            MockLibraryUtil.compileJavaFilesLibraryToJar(
                TEST_ANNOTATIONS_SOURCE_PATH,
                "test-foreign-annotations",
                extraOptions = listOf("-Xallow-kotlin-package"),
                extraClasspath = extraClassPath.map { it.path },
                assertions = JUnit5Assertions
            )
        )

    override fun provideAdditionalAnalysisFlags(directives: RegisteredDirectives): Map<AnalysisFlag<*>, Any?> {
        val globalState = directives.singleOrZeroValue(JSR305_GLOBAL_REPORT) ?: ReportLevel.STRICT
        val migrationState = directives.singleOrZeroValue(JSR305_MIGRATION_REPORT)
        val userAnnotationsState = directives[JSR305_SPECIAL_REPORT].mapNotNull {
            val (name, stateDescription) = it.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
            val state = ReportLevel.findByDescription(stateDescription) ?: return@mapNotNull null
            name to state
        }.toMap()
        val jSpecifyReportLevel = directives.singleOrZeroValue(JSPECIFY_STATE) ?: ReportLevel.STRICT
        return mapOf(
            JvmAnalysisFlags.javaTypeEnhancementState to JavaTypeEnhancementState(
                globalState,
                migrationState,
                userAnnotationsState,
                jspecifyReportLevel = jSpecifyReportLevel
            )
        )
    }
}

class JvmForeignAnnotationsAgainstCompiledJavaConfigurator(testServices: TestServices) : JvmForeignAnnotationsConfigurator(testServices) {
    @OptIn(ExperimentalStdlibApi::class)
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val compiledJavaPath = testServices.createTempDirectory("java-compiled-files")

        val foreignAnnotations = createJarWithForeignAnnotations(module)
        val testAnnotations = compileTestAnnotations(foreignAnnotations)
        val additionalClasspath = buildList {
            addAll(foreignAnnotations)
            addAll(testAnnotations)
        }.map { it.path }

        module.javaFiles.forEach { testServices.sourceFileProvider.getRealFileForSourceFile(it) }
        CodegenTestUtil.compileJava(
            CodegenTestUtil.findJavaSourcesInDirectory(testServices.sourceFileProvider.javaSourceDirectory),
            additionalClasspath,
            emptyList(),
            compiledJavaPath,
            JUnit5Assertions
        )

        val extraClassPath = buildList {
            add(compiledJavaPath)
            addAll(testAnnotations)
        }

        configuration.addJvmClasspathRoots(extraClassPath)
    }
}
