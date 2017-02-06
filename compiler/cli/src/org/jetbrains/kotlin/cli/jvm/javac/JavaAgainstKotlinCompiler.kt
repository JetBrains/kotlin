/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.jvm.javac

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.kotlinSourceRoots
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.io.File
import java.io.StringWriter
import java.nio.charset.Charset
import java.util.*
import javax.tools.*

object JavaAgainstKotlinCompiler {

    private val VirtualFile.javaFiles: List<VirtualFile>
        get() = children.filter { it.extension == "java" }
                .toMutableList()
                .apply {
                    children
                            .filter(VirtualFile::isDirectory)
                            .forEach { dir -> addAll(dir.javaFiles) }
                }

    private val KotlinCoreEnvironment.javaFiles
        get() = configuration.kotlinSourceRoots
                .mapNotNull { findLocalDirectory(it) }
                .flatMap { it.javaFiles }
                .map { File(it.canonicalPath) }

    private val CompilerConfiguration.javacOptions: List<String>
        get() = listOf("-target", this[JVMConfigurationKeys.JVM_TARGET]?.name ?: "1.8")


    private fun KotlinCoreEnvironment.enablePartialAnalysis() = AnalysisHandlerExtension.registerExtension(project, PartialAnalysisHandlerExtension())

    private fun KotlinCoreEnvironment.analyze(files: Collection<KtFile> = getSourceFiles()): AnalysisResult {
        files.forEach { AnalyzingUtils.checkForSyntacticErrors(it) }

        return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                project, files, CliLightClassGenerationSupport.CliBindingTrace(),
                configuration, { scope ->
                    JvmPackagePartProvider(this, scope)
                }
        ).apply {
            AnalyzingUtils.throwExceptionOnErrors(bindingContext)
        }
    }

    private fun KotlinCoreEnvironment.compileKotlinFiles(ktFiles: List<KtFile> = getSourceFiles()): Iterable<OutputFile> {
        enablePartialAnalysis()

        val analysisResult = analyze(ktFiles).apply { throwIfError() }
        val generationState = GenerationState(
                project,
                ClassBuilderFactories.LIGHT,
                analysisResult.moduleDescriptor,
                analysisResult.bindingContext,
                ktFiles,
                configuration
        )

        if (analysisResult.shouldGenerateCode) {
            KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION)
        }
        AnalyzingUtils.throwExceptionOnErrors(generationState.collectedExtraJvmDiagnostics)

        return generationState.factory.getClassFiles()
    }

    private fun Iterable<OutputFile>.toKotlinLightClasses(ktFiles: List<KtFile>) = ktFiles.flatMap { ktFile ->
        filter { it.sourceFiles.firstOrNull()?.canonicalPath == ktFile.originalFile.virtualFile.canonicalPath }
                .map {
                    val packageFqName = ktFile.packageFqName.asString()
                    val binaryName = packageFqName + "." +
                                     it.relativePath.replace("/", ".")
                                             .substringBeforeLast(".")
                                             .substring(packageFqName.length + 1)
                                             .replace(".", "$")

                    KotlinLightClass(binaryName, packageFqName, it.asByteArray())
                }
    }

    @JvmStatic
    fun compileJavaFiles(environment: KotlinCoreEnvironment,
                         messageCollector: MessageCollector,
                         destination: String) {

        messageCollector.report(CompilerMessageSeverity.INFO,
                                "Parallel Java against Kotlin compiler",
                                CompilerMessageLocation.NO_LOCATION)

        val configuration = environment.configuration
        val classpath = configuration.jvmClasspathRoots

        val javaFiles = environment.javaFiles.check { it.isNotEmpty() } ?: return
        val outDir = File(destination).apply { mkdirs() }

        val options = configuration.javacOptions

        val javac = ToolProvider.getSystemJavaCompiler()
        val lightClasses = environment.compileKotlinFiles().toKotlinLightClasses(environment.getSourceFiles())

        val diagnosticCollector = DiagnosticCollector<JavaFileObject>()
        val fileManager = KotlinFileManager(
                javac.getStandardFileManager(diagnosticCollector, Locale.ENGLISH, Charset.forName("utf-8")),
                lightClasses
        )

        with (fileManager) {
            val javaFileObjects = standardFileManager.getJavaFileObjectsFromFiles(javaFiles)

            standardFileManager.setLocation(StandardLocation.CLASS_PATH, classpath)
            standardFileManager.setLocation(StandardLocation.CLASS_OUTPUT, listOf(outDir))

            use { fileManager ->
                val compilationTask = javac.getTask(StringWriter(), fileManager, diagnosticCollector,
                                                    options, null, javaFileObjects)
                if (!compilationTask.call()) {
                    diagnosticCollector.diagnostics.forEach {
                        val severity = when (it.kind) {
                            Diagnostic.Kind.ERROR -> CompilerMessageSeverity.ERROR
                            Diagnostic.Kind.WARNING -> CompilerMessageSeverity.WARNING
                            Diagnostic.Kind.MANDATORY_WARNING -> CompilerMessageSeverity.STRONG_WARNING
                            Diagnostic.Kind.NOTE -> CompilerMessageSeverity.INFO
                            else -> CompilerMessageSeverity.LOGGING
                        }

                        val message = "${it.source.name} [${it.lineNumber}, ${it.columnNumber}]: ${it.getMessage(Locale.ENGLISH)}"
                        messageCollector.report(severity, message, CompilerMessageLocation.NO_LOCATION)
                    }
                }
            }
        }
    }

    // for tests only
    @JvmStatic
    fun compileJavaFiles(javaFiles: List<File>,
                         kotlinFiles: List<File>,
                         environment: KotlinCoreEnvironment,
                         outDir: File) {

        fun createKtFile(name: String, text: String, project: Project): KtFile {
            val shortName = name.substringAfterLast("/").substringAfterLast("\\")

            val virtualFile = object : LightVirtualFile(shortName, KotlinLanguage.INSTANCE, text) {
                override fun getPath() = "/$name"
            }
            virtualFile.charset = CharsetToolkit.UTF8_CHARSET

            val factory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl

            return factory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile
        }

        val javac = ToolProvider.getSystemJavaCompiler()
        val ktFiles = kotlinFiles.map {
            createKtFile(it.name, FileUtil.loadFile(it, true), environment.project)
        }

        val outputFiles = environment.compileKotlinFiles(ktFiles)
        val lightClasses = outputFiles.toKotlinLightClasses(ktFiles)

        val diagnosticCollector = DiagnosticCollector<JavaFileObject>()
        val fileManager = KotlinFileManager(
                javac.getStandardFileManager(diagnosticCollector, Locale.ENGLISH, Charset.forName("utf-8")),
                lightClasses
        )

        val javaFileObjects = fileManager.standardFileManager.getJavaFileObjectsFromFiles(javaFiles)

        fileManager.use { fileManager ->
            val compilationTask = javac.getTask(StringWriter(), fileManager, diagnosticCollector,
                                                listOf("-d", outDir.absolutePath), null, javaFileObjects)
            compilationTask.call()
        }
    }

}