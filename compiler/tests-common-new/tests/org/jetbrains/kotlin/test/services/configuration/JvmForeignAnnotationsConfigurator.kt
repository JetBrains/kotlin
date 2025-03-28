/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.cli.jvm.addModularRootIfNotNull
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_THIRDPARTY_ANNOTATIONS_PATH
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_THIRDPARTY_JAVA8_ANNOTATIONS_PATH
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_THIRDPARTY_JAVA9_ANNOTATIONS_PATH
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_THIRDPARTY_JSR305_PATH
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives.ENABLE_FOREIGN_ANNOTATIONS
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
import java.net.URI
import java.util.zip.ZipFile
import kotlin.io.path.createTempDirectory

enum class JavaForeignAnnotationType(val path: String) {
    Annotations(System.getProperty(KOTLIN_THIRDPARTY_ANNOTATIONS_PATH) ?: "third-party/annotations"),
    Java8Annotations(System.getProperty(KOTLIN_THIRDPARTY_JAVA8_ANNOTATIONS_PATH) ?: "third-party/java8-annotations"),
    Java9Annotations(System.getProperty(KOTLIN_THIRDPARTY_JAVA9_ANNOTATIONS_PATH) ?: "third-party/java9-annotations"),
    Jsr305(System.getProperty(KOTLIN_THIRDPARTY_JSR305_PATH) ?: "third-party/jsr305")
}

open class JvmForeignAnnotationsConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        const val JSR_305_TEST_ANNOTATIONS_PATH = "diagnostics/helpers/jsr305_test_annotations"
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(ForeignAnnotationsDirectives)

    override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion
    ): Map<AnalysisFlag<*>, Any?> {
        if (ENABLE_FOREIGN_ANNOTATIONS !in directives) return emptyMap()
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
        if (ENABLE_FOREIGN_ANNOTATIONS !in registeredDirectives) return

        val annotationPath = registeredDirectives[ForeignAnnotationsDirectives.ANNOTATIONS_PATH].singleOrNull()
            ?: JavaForeignAnnotationType.Java8Annotations
        val javaFilesDir = createTempDirectory().toFile().also {
            File(annotationPath.path).copyRecursively(it)
        }
        val jsr305JarFile = createJsr305Jar(configuration)
        val useJava11ToCompileIncludedJavaFiles =
            registeredDirectives[JvmEnvironmentConfigurationDirectives.JDK_KIND].singleOrNull() == TestJdkKind.FULL_JDK_11
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
            val resourceUri = this::class.java.classLoader.getResource(JSR_305_TEST_ANNOTATIONS_PATH)!!.toURI()
            val target = createTempDirectory().toFile()
            when (resourceUri.scheme) {
                "jar" -> {
                    val array = resourceUri.toString().split("!")
                    val jarUri = URI.create(array[0])
                    val pathInsideJar = array[1]
                    val path = jarUri.toString().substringAfterLast(":")
                    ZipFile(path).use { zipFile ->
                        val prefix = pathInsideJar.removePrefix("/")
                        zipFile.entries().asSequence()
                            .filter { entry -> !entry.isDirectory && entry.name.startsWith(prefix) }
                            .forEach { entry ->
                                val relativePath = entry.name.removePrefix(prefix)
                                val targetFile = File(target, relativePath)
                                targetFile.parentFile.mkdirs()
                                zipFile.getInputStream(entry).use { input ->
                                    targetFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                    }
                }
                "file" -> File(resourceUri).copyRecursively(target)
                else -> throw UnsupportedOperationException("Unsupported URI scheme: ${resourceUri.scheme}")
            }
            configuration.addJvmClasspathRoot(
                MockLibraryUtil.compileJavaFilesLibraryToJar(
                    target.path,
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
