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

import com.intellij.openapi.vfs.VirtualFile
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
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclarationContainer
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

class JavaAgainstKotlinCompiler(private val environment: KotlinCoreEnvironment) {

    private val outputFiles = hashMapOf<KtFile, Iterable<OutputFile>>()

    private lateinit var analysisResult: AnalysisResult

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
        get() = this[JVMConfigurationKeys.JVM_TARGET]?.name?.let { listOf("-target", it) } ?: emptyList()

    private fun KotlinCoreEnvironment.enablePartialAnalysis() = AnalysisHandlerExtension.registerExtension(project, PartialAnalysisHandlerExtension())

    private fun KotlinLightClass.toByteCode(): Iterable<OutputFile> {
        val generationState = GenerationState(
                environment.project,
                ClassBuilderFactories.LIGHT,
                analysisResult.moduleDescriptor,
                analysisResult.bindingContext,
                listOf(ktFile),
                environment.configuration
        )

        if (analysisResult.shouldGenerateCode) {
            KotlinCodegenFacade.compileCorrectFiles(generationState, CompilationErrorHandler.THROW_EXCEPTION)
        }
        AnalyzingUtils.throwExceptionOnErrors(generationState.collectedExtraJvmDiagnostics)

        return generationState.factory.getClassFiles().let {
            outputFiles.put(ktFile, it)
            it
        }
    }

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

    private fun KtDeclarationContainer.getAllClassesFqNames(): List<String> = arrayListOf<String?>()
            .also { list ->
                when {
                    this is KtFile -> list.add(javaFileFacadeFqName.asString())
                    this is KtClassOrObject -> list.add(fqName?.asString())
                }

                declarations
                        .filterIsInstance<KtClassOrObject>()
                        .forEach {
                            list.addAll(it.getAllClassesFqNames())
                        }
            }.filterNotNull()

    private fun toKotlinLightClasses(ktFile: KtFile) = ktFile.getAllClassesFqNames()
            .map {
                val packageFqName = ktFile.packageFqName.asString()
                val binaryName = packageFqName + "." +
                                 it.substring(packageFqName.length + 1).replace(".", "$")

                KotlinLightClass(binaryName, packageFqName, ktFile, this)
            }

    fun getByteCode(lightClass: KotlinLightClass) = (outputFiles[lightClass.ktFile] ?: lightClass.toByteCode())
            .first { lightClass.binaryName == it.relativePath.replace("/", ".").substringBeforeLast('.') }
            .asByteArray()

    fun compileJavaFiles(javaFiles: List<File>? = environment.javaFiles.check { it.isNotEmpty() },
                         ktFiles: List<KtFile> = environment.getSourceFiles(),
                         destination: String,
                         messageCollector: MessageCollector?) {
        javaFiles ?: return

        messageCollector?.report(CompilerMessageSeverity.INFO,
                                "Parallel Java against Kotlin compiler",
                                CompilerMessageLocation.NO_LOCATION)

        environment.enablePartialAnalysis()

        val configuration = environment.configuration
        val classpath = configuration.jvmClasspathRoots
        val outDir = File(destination).apply { mkdirs() }
        val options = configuration.javacOptions

        analysisResult = environment.analyze(ktFiles)

        val javac = ToolProvider.getSystemJavaCompiler()
        val lightClasses = ktFiles.flatMap { toKotlinLightClasses(it) }

        val diagnosticCollector = DiagnosticCollector<JavaFileObject>()
        val fileManager = KotlinFileManager(
                javac.getStandardFileManager(diagnosticCollector, Locale.ENGLISH, Charset.forName("utf-8")),
                lightClasses
        )

        with(fileManager) {
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
                        messageCollector?.report(severity, message, CompilerMessageLocation.NO_LOCATION)
                    }
                }
            }
        }
    }

}
