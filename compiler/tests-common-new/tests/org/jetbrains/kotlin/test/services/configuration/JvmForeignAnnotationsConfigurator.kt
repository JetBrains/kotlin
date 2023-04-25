/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.cli.jvm.addModularRootIfNotNull
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.name.FqName
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
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.standardLibrariesPathProvider
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import kotlin.io.path.createTempDirectory

enum class JavaForeignAnnotationType(val path: String) {
    Annotations("third-party/annotations"),
    Java8Annotations("third-party/java8-annotations"),
    Java9Annotations("third-party/java9-annotations"),
    Jsr305("third-party/jsr305");
}

open class JvmForeignAnnotationsConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        const val JSR_305_TEST_ANNOTATIONS_PATH = "compiler/testData/diagnostics/helpers/jsr305_test_annotations"
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(ForeignAnnotationsDirectives)

    @OptIn(ExperimentalStdlibApi::class)
    override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion
    ): Map<AnalysisFlag<*>, Any?> {
        val defaultJsr305Settings = getDefaultJsr305Settings(languageVersion.toKotlinVersion())
        val globalState = directives.singleOrZeroValue(JSR305_GLOBAL_REPORT) ?: defaultJsr305Settings.globalLevel
        val migrationState = directives.singleOrZeroValue(JSR305_MIGRATION_REPORT) ?: defaultJsr305Settings.migrationLevel
        val userAnnotationsState = directives[JSR305_SPECIAL_REPORT].mapNotNull {
            val (name, stateDescription) = it.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
            val state = ReportLevel.findByDescription(stateDescription) ?: return@mapNotNull null
            FqName(name) to state
        }.toMap()
        val configuredReportLevels = NullabilityAnnotationStatesImpl(
            buildMap<FqName, ReportLevel> {
                directives.singleOrZeroValue(JSPECIFY_STATE)?.let { 
                    put(JSPECIFY_OLD_ANNOTATIONS_PACKAGE, it)
                    put(JSPECIFY_ANNOTATIONS_PACKAGE, it)
                }
                for ((fqname, reportLevel) in directives[ForeignAnnotationsDirectives.NULLABILITY_ANNOTATIONS]) {
                    put(fqname, reportLevel)
                }
            }
        )

        return mapOf(
            JvmAnalysisFlags.javaTypeEnhancementState to JavaTypeEnhancementState(
                Jsr305Settings(globalState, migrationState, userAnnotationsState),
                getReportLevelForAnnotation = { getReportLevelForAnnotation(it, configuredReportLevels) }
            )
        )
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val registeredDirectives = module.directives
        val javaVersionToCompile = registeredDirectives[JvmEnvironmentConfigurationDirectives.COMPILE_JAVA_USING].singleOrNull()
        val useJava11ToCompileIncludedJavaFiles = javaVersionToCompile == TestJavacVersion.JAVAC_11
        val annotationPath = registeredDirectives[ForeignAnnotationsDirectives.ANNOTATIONS_PATH].singleOrNull()
            ?: JavaForeignAnnotationType.Java8Annotations
        val javaFilesDir = createTempDirectory().toFile().also {
            File(annotationPath.path).copyRecursively(it)
        }

        val jsr305JarFile = createJsr305Jar(configuration)

        val foreignAnnotationsJar = MockLibraryUtil.compileJavaFilesLibraryToJar(
            javaFilesDir.path,
            "foreign-annotations",
            assertions = JUnit5Assertions,
            extraClasspath = configuration.jvmClasspathRoots.map { it.absolutePath } + jsr305JarFile.absolutePath,
            useJava11 = useJava11ToCompileIncludedJavaFiles
        )
        configuration.addModularRootIfNotNull(useJava11ToCompileIncludedJavaFiles, "java9_annotations", foreignAnnotationsJar)
        testServices.register(AdditionalClassPathForJavaCompilationOrAnalysis::class, AdditionalClassPathForJavaCompilationOrAnalysis(listOf(jsr305JarFile.absolutePath)))
        configuration.addJvmClasspathRoot(testServices.standardLibrariesPathProvider.jvmAnnotationsForTests())

        if (JvmEnvironmentConfigurationDirectives.WITH_JSR305_TEST_ANNOTATIONS in registeredDirectives) {
            val jsr305AnnotationsDir = createTempDirectory().toFile().also {
                File(JSR_305_TEST_ANNOTATIONS_PATH).copyRecursively(it)
            }
            configuration.addJvmClasspathRoot(
                MockLibraryUtil.compileJavaFilesLibraryToJar(
                    jsr305AnnotationsDir.path,
                    "jsr-305-test-annotations",
                    assertions = JUnit5Assertions,
                    extraClasspath = configuration.jvmClasspathRoots.map { it.absolutePath } + jsr305JarFile.absolutePath
                )
            )
            configuration.addJvmClasspathRoot(KtTestUtil.getAnnotationsJar())
        }
    }

    private fun createJsr305Jar(configuration: CompilerConfiguration): File {
        val jsr305FilesDir = createTempDirectory().toFile().also {
            File(JavaForeignAnnotationType.Jsr305.path).copyRecursively(it)
        }

        return MockLibraryUtil.compileJavaFilesLibraryToJar(
            jsr305FilesDir.path,
            "jsr305",
            assertions = JUnit5Assertions,
            extraClasspath = configuration.jvmClasspathRoots.map { it.absolutePath },
        )
    }
}
