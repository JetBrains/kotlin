/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.cli.jvm.addModularRootIfNotNull
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.TestJavacVersion
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.JSPECIFY_STATE
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.JSR305_GLOBAL_REPORT
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.JSR305_MIGRATION_REPORT
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.JSR305_SPECIAL_REPORT
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.JavaTypeEnhancementState
import org.jetbrains.kotlin.utils.ReportLevel
import java.io.File
import kotlin.io.path.createTempDirectory

enum class JavaForeignAnnotationType(val path: String) {
    Annotations("third-party/annotations"),
    Java8Annotations("third-party/java8-annotations"),
    Java9Annotations("third-party/java9-annotations");
}

open class JvmForeignAnnotationsConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        const val JSR_305_TEST_ANNOTATIONS_PATH = "compiler/testData/diagnostics/helpers/jsr305_test_annotations"
    }

    override val directivesContainers: List<DirectivesContainer>
        get() = listOf(ForeignAnnotationsDirectives)

    override fun provideAdditionalAnalysisFlags(directives: RegisteredDirectives): Map<AnalysisFlag<*>, Any?> {
        val globalState = directives.singleOrZeroValue(JSR305_GLOBAL_REPORT) ?: ReportLevel.WARN
        val migrationState = directives.singleOrZeroValue(JSR305_MIGRATION_REPORT)
        val userAnnotationsState = directives[JSR305_SPECIAL_REPORT].mapNotNull {
            val (name, stateDescription) = it.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
            val state = ReportLevel.findByDescription(stateDescription) ?: return@mapNotNull null
            name to state
        }.toMap()
        val jSpecifyReportLevel = directives.singleOrZeroValue(JSPECIFY_STATE) ?: ReportLevel.WARN
        return mapOf(
            JvmAnalysisFlags.javaTypeEnhancementState to JavaTypeEnhancementState(
                globalState,
                migrationState,
                userAnnotationsState,
                jspecifyReportLevel = jSpecifyReportLevel
            )
        )
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val registeredDirectives = module.directives
        val javaVersionToCompile = registeredDirectives[JvmEnvironmentConfigurationDirectives.COMPILE_JAVA_USING].singleOrNull()
        val useJava9ToCompileIncludedJavaFiles = javaVersionToCompile == TestJavacVersion.JAVAC_9
        val annotationPath = registeredDirectives[ForeignAnnotationsDirectives.ANNOTATIONS_PATH].singleOrNull()
            ?: JavaForeignAnnotationType.Java8Annotations
        val javaFilesDir = createTempDirectory().toFile().also {
            File(annotationPath.path).copyRecursively(it)
        }
        val foreignAnnotationsJar = MockLibraryUtil.compileJavaFilesLibraryToJar(
            javaFilesDir.path,
            "foreign-annotations",
            assertions = JUnit5Assertions,
            extraClasspath = configuration.jvmClasspathRoots.map { it.absolutePath },
            useJava9 = useJava9ToCompileIncludedJavaFiles
        )
        configuration.addModularRootIfNotNull(useJava9ToCompileIncludedJavaFiles, "java9_annotations", foreignAnnotationsJar)
        configuration.addJvmClasspathRoot(ForTestCompileRuntime.jvmAnnotationsForTests())

        if (JvmEnvironmentConfigurationDirectives.WITH_JSR305_TEST_ANNOTATIONS in registeredDirectives) {
            val jsr305AnnotationsDir = createTempDirectory().toFile().also {
                File(JSR_305_TEST_ANNOTATIONS_PATH).copyRecursively(it)
            }
            configuration.addJvmClasspathRoot(
                MockLibraryUtil.compileJavaFilesLibraryToJar(
                    jsr305AnnotationsDir.path,
                    "jsr-305-test-annotations",
                    assertions = JUnit5Assertions,
                    extraClasspath = configuration.jvmClasspathRoots.map { it.absolutePath }
                )
            )
            configuration.addJvmClasspathRoot(KtTestUtil.getAnnotationsJar())
        }
    }
}
