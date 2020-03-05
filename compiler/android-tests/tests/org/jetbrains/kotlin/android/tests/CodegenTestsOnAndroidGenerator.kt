/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.tests

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.CodegenTestFiles
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.*
import org.junit.Assert
import org.junit.Ignore
import java.io.File
import java.io.FileWriter
import java.io.IOException

data class ConfigurationKey(val kind: ConfigurationKind, val jdkKind: TestJdkKind, val configuration: String)

@Ignore
class CodegenTestsOnAndroidGenerator private constructor(private val pathManager: PathManager) : CodegenTestCase() {
    private var writtenFilesCount = 0

    private var currentModuleIndex = 1

    private val pathFilter: String? = System.getProperties().getProperty("kotlin.test.android.path.filter")

    private val pendingUnitTestGenerators = hashMapOf<Int, UnitTestFileWriter>()

    //keep it globally to avoid test grouping on TC
    private val generatedTestNames = hashSetOf<String>()

    fun getFlavorUnitTestFilePath(index: Int): String {
        return pathManager.srcFolderInAndroidTmpFolder + "/androidTestKtest$index/java/" + testClassPackage.replace(
            ".",
            "/"
        ) + "/" + testClassName + "$index.java"
    }

    private fun prepareAndroidModuleAndGenerateTests() {
        prepareAndroidModule()
        generateTestsAndFlavourSuites()
    }

    private fun prepareAndroidModule() {
        FileUtil.copyDir(File(pathManager.androidModuleRoot), File(pathManager.tmpFolder))
        writeAndroidSkdToLocalProperties(pathManager)

        println("Copying kotlin-stdlib.jar and kotlin-reflect.jar in android module...")
        copyKotlinRuntimeJars()
    }

    private fun copyKotlinRuntimeJars() {
        FileUtil.copy(
            ForTestCompileRuntime.runtimeJarForTests(),
            File(pathManager.libsFolderInAndroidTmpFolder + "/kotlin-stdlib.jar")
        )
        FileUtil.copy(
            ForTestCompileRuntime.reflectJarForTests(),
            File(pathManager.libsFolderInAndroidTmpFolder + "/kotlin-reflect.jar")
        )

        FileUtil.copy(
            ForTestCompileRuntime.kotlinTestJarForTests(),
            File(pathManager.libsFolderInAndroidTmpFolder + "/kotlin-test.jar")
        )
    }

    private fun generateTestsAndFlavourSuites() {
        println("Generating test files...")

        generateTestMethodsForDirectories(File("compiler/testData/codegen/box"), File("compiler/testData/codegen/boxInline"))

        pendingUnitTestGenerators.values.forEach { it.generate() }
    }

    private fun generateTestMethodsForDirectories(vararg dirs: File) {
        val holders = mutableMapOf<ConfigurationKey, FilesWriter>()

        for (dir in dirs) {
            val files = dir.listFiles() ?: error("Folder with testData is empty: ${dir.absolutePath}")
            processFiles(files, holders)
        }

        holders.values.forEach {
            it.writeFilesOnDisk()
        }
    }

    internal inner class FilesWriter(
        private val configuration: CompilerConfiguration
    ) {
        private val rawFiles = arrayListOf<TestClassInfo>()
        private val unitTestDescriptions = arrayListOf<TestInfo>()

        private fun shouldWriteFilesOnDisk(): Boolean = rawFiles.size > 300

        fun writeFilesOnDiskIfNeeded() {
            if (shouldWriteFilesOnDisk()) {
                writeFilesOnDisk()
            }
        }

        fun writeFilesOnDisk() {
            val disposable = TestDisposable()

            val environment = KotlinCoreEnvironment.createForTests(
                disposable,
                configuration.copy().apply { put(CommonConfigurationKeys.MODULE_NAME, "android-module-" + currentModuleIndex++) },
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )

            writeFiles(
                rawFiles.map {
                    try {
                        CodegenTestFiles.create(it.name, it.content, environment.project).psiFile
                    } catch (e: Throwable) {
                        throw RuntimeException("Error on processing ${it.name}:\n${it.content}", e)
                    }
                }, environment
            )
            Disposer.dispose(disposable)
            rawFiles.clear()
            unitTestDescriptions.clear()
        }

        private fun writeFiles(filesToCompile: List<KtFile>, environment: KotlinCoreEnvironment) {
            if (filesToCompile.isEmpty()) return

            //2500 files per folder that would be used by flavor to avoid multidex usage,
            // each folder would be jared by build.gradle script
            writtenFilesCount += filesToCompile.size
            val index = writtenFilesCount / 2500
            val outputDir = File(pathManager.getOutputForCompiledFiles(index))
            assertTrue("Add flavors for ktest$index", index < 3)

            println("Generating ${filesToCompile.size} files into ${outputDir.name}, configuration: '${environment.configuration}'...")

            val outputFiles = GenerationUtils.compileFiles(filesToCompile, environment).run { destroy(); factory }

            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            Assert.assertTrue("Cannot create directory for compiled files", outputDir.exists())
            val unitTestFileWriter = pendingUnitTestGenerators.getOrPut(index) {
                UnitTestFileWriter(
                    getFlavorUnitTestFilePath(index),
                    index,
                    generatedTestNames
                )
            }
            unitTestFileWriter.addTests(unitTestDescriptions)
            outputFiles.writeAllTo(outputDir)
        }

        fun addTest(testFiles: List<TestClassInfo>, info: TestInfo) {
            rawFiles.addAll(testFiles)
            unitTestDescriptions.add(info)
        }
    }

    @Throws(IOException::class)
    private fun processFiles(
        files: Array<File>,
        holders: MutableMap<ConfigurationKey, FilesWriter>
    ) {
        holders.values.forEach {
            it.writeFilesOnDiskIfNeeded()
        }

        for (file in files) {
            if (SpecialFiles.getExcludedFiles().contains(file.name)) {
                continue
            }
            if (file.isDirectory) {
                val listFiles = file.listFiles()
                if (listFiles != null) {
                    processFiles(listFiles, holders)
                }
            } else if (FileUtilRt.getExtension(file.name) != KotlinFileType.EXTENSION) {
                // skip non kotlin files
            } else {
                if (pathFilter != null && !file.path.contains(pathFilter)) {
                    continue
                }

                if (!InTextDirectivesUtils.isPassingTarget(TargetBackend.JVM, file)) {
                    continue
                }

                val fullFileText =
                    FileUtil.loadFile(file, true).replace("COROUTINES_PACKAGE", "kotlin.coroutines")

                if (fullFileText.contains("// WITH_COROUTINES")) {
                    if (fullFileText.contains("kotlin.coroutines.experimental")) continue
                    if (fullFileText.contains("// LANGUAGE_VERSION: 1.2")) continue
                }

                //TODO support JvmPackageName
                if (fullFileText.contains("@file:JvmPackageName(")) continue
                // TODO: Support jvm assertions
                if (fullFileText.contains("// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm")) continue
                // TODO: support JVM 8 test with D8
                if (fullFileText.contains("// JVM_TARGET")) continue
                // TODO: support SKIP_JDK6 on new platforms
                if (fullFileText.contains("// SKIP_JDK6")) continue

                if (hasBoxMethod(fullFileText)) {
                    val testFiles = createTestFiles(file, fullFileText)
                    val kind = extractConfigurationKind(testFiles)
                    val jdkKind = getTestJdkKind(testFiles)
                    val keyConfiguration = CompilerConfiguration()
                    updateConfigurationByDirectivesInTestFiles(testFiles, keyConfiguration)

                    val key = ConfigurationKey(kind, jdkKind, keyConfiguration.toString())
                    val filesHolder = holders.getOrPut(key) {
                        FilesWriter(KotlinTestUtils.newConfiguration(kind, jdkKind, KotlinTestUtils.getAnnotationsJar()).apply {
                            println("Creating new configuration by $key")
                            updateConfigurationByDirectivesInTestFiles(testFiles, this)
                        })
                    }

                    patchFilesAndAddTest(file, testFiles, filesHolder)
                }
            }
        }
    }

    private fun createTestFiles(file: File, expectedText: String): List<TestFile> =
        TestFiles.createTestFiles(
            file.name,
            expectedText,
            object : TestFiles.TestFileFactoryNoModules<TestFile>() {
                override fun create(fileName: String, text: String, directives: Map<String, String>): TestFile {
                    return TestFile(fileName, text, directives)
                }
            }, false,
            "kotlin.coroutines"
        )

    companion object {
        const val testClassPackage = "org.jetbrains.kotlin.android.tests"
        const val testClassName = "CodegenTestCaseOnAndroid"
        const val baseTestClassPackage = "org.jetbrains.kotlin.android.tests"
        const val baseTestClassName = "AbstractCodegenTestCaseOnAndroid"
        const val generatorName = "CodegenTestsOnAndroidGenerator"


        @JvmStatic
        @Throws(Throwable::class)
        fun generate(pathManager: PathManager) {
            CodegenTestsOnAndroidGenerator(pathManager).prepareAndroidModuleAndGenerateTests()
        }

        private fun hasBoxMethod(text: String): Boolean {
            return text.contains("fun box()")
        }

        @Throws(IOException::class)
        internal fun writeAndroidSkdToLocalProperties(pathManager: PathManager) {
            val sdkRoot = File(pathManager.androidSdkRoot).invariantSeparatorsPath
            println("Writing android sdk to local.properties: $sdkRoot")
            val file = File(pathManager.tmpFolder + "/local.properties")
            FileWriter(file).use { fw -> fw.write("sdk.dir=$sdkRoot") }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val tmpFolder = createTempDir()
            println("Created temporary folder for android tests: " + tmpFolder.absolutePath)
            val rootFolder = File("")
            val pathManager = PathManager(rootFolder.absolutePath, tmpFolder.absolutePath)
            generate(pathManager)
        }
    }
}
