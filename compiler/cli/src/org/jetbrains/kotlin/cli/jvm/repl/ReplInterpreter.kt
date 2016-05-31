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

package org.jetbrains.kotlin.cli.jvm.repl

import com.google.common.base.Throwables
import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.repl.CliReplAnalyzerEngine.ReplLineAnalysisResult.Successful
import org.jetbrains.kotlin.cli.jvm.repl.CliReplAnalyzerEngine.ReplLineAnalysisResult.WithErrors
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.ScriptParameter
import org.jetbrains.kotlin.script.StandardScriptDefinition
import java.io.PrintWriter
import java.net.URLClassLoader

class ReplInterpreter(
        disposable: Disposable,
        private val configuration: CompilerConfiguration,
        private val replConfiguration: ReplConfiguration
): ReplConfiguration by replConfiguration {
    private var lineNumber = 0

    private val earlierLines = arrayListOf<EarlierLine>()
    private val previousIncompleteLines = arrayListOf<String>()
    private val classLoader: ReplClassLoader = run {
        val classpath = configuration.jvmClasspathRoots.map { it.toURI().toURL() }
        ReplClassLoader(URLClassLoader(classpath.toTypedArray(), null))
    }

    private val environment = run {
        configuration.add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, REPL_LINE_AS_SCRIPT_DEFINITION)
        KotlinCoreEnvironment.createForProduction(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    private val psiFileFactory: PsiFileFactoryImpl = PsiFileFactory.getInstance(environment.project) as PsiFileFactoryImpl
    private val analyzerEngine = CliReplAnalyzerEngine(environment)

    enum class LineResultType {
        SUCCESS,
        COMPILE_ERROR,
        RUNTIME_ERROR,
        INCOMPLETE
    }

    class LineResult private constructor(
            private val resultingValue: Any?,
            private val unit: Boolean,
            val errorText: String?,
            val type: LineResultType
    ) {
        val value: Any?
            get() {
                checkSuccessful()
                return resultingValue
            }

        val isUnit: Boolean
            get() {
                checkSuccessful()
                return unit
            }

        private fun checkSuccessful() {
            if (type != LineResultType.SUCCESS) {
                error("it is error")
            }
        }

        companion object {
            private fun error(errorText: String, errorType: LineResultType): LineResult {
                val resultingErrorText = when {
                    errorText.isEmpty() -> "<unknown error>"
                    !errorText.endsWith("\n") -> errorText + "\n"
                    else -> errorText
                }

                return LineResult(null, false, resultingErrorText, errorType)
            }

            fun successful(value: Any?, unit: Boolean): LineResult {
                return LineResult(value, unit, null, LineResultType.SUCCESS)
            }

            fun compileError(errorText: String): LineResult {
                return error(errorText, LineResultType.COMPILE_ERROR)
            }

            fun runtimeError(errorText: String): LineResult {
                return error(errorText, LineResultType.RUNTIME_ERROR)
            }

            fun incomplete(): LineResult {
                return LineResult(null, false, null, LineResultType.INCOMPLETE)
            }
        }
    }

    fun eval(line: String): LineResult {
        ++lineNumber

        val fullText = (previousIncompleteLines + line).joinToString(separator = "\n")

        val virtualFile =
                LightVirtualFile("line$lineNumber${KotlinParserDefinition.STD_SCRIPT_EXT}", KotlinLanguage.INSTANCE, fullText).apply {
                    charset = CharsetToolkit.UTF8_CHARSET
                }
        val psiFile = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile?
                      ?: error("Script file not analyzed at line $lineNumber: $fullText")

        val errorHolder = createDiagnosticHolder()

        val syntaxErrorReport = AnalyzerWithCompilerReport.reportSyntaxErrors(psiFile, errorHolder)

        if (syntaxErrorReport.isHasErrors && syntaxErrorReport.isAllErrorsAtEof) {
            return if (allowIncompleteLines) {
                previousIncompleteLines.add(line)
                LineResult.incomplete()
            }
            else {
                LineResult.compileError(errorHolder.renderedDiagnostics)
            }
        }

        previousIncompleteLines.clear()

        if (syntaxErrorReport.isHasErrors) {
            return LineResult.compileError(errorHolder.renderedDiagnostics)
        }

        val analysisResult = analyzerEngine.analyzeReplLine(psiFile, lineNumber)
        AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder, false)
        val scriptDescriptor = when (analysisResult) {
            is WithErrors -> return LineResult.compileError(errorHolder.renderedDiagnostics)
            is Successful -> analysisResult.scriptDescriptor
            else -> error("Unexpected result ${analysisResult.javaClass}")
        }

        val state = GenerationState(psiFile.project, ClassBuilderFactories.BINARIES, analyzerEngine.module, analyzerEngine.trace.bindingContext, listOf(psiFile))

        compileScript(psiFile.script!!, earlierLines.map(EarlierLine::getScriptDescriptor), state, CompilationErrorHandler.THROW_EXCEPTION)

        for (outputFile in state.factory.asList()) {
            if (outputFile.relativePath.endsWith(".class")) {
                classLoader.addClass(JvmClassName.byInternalName(outputFile.relativePath.replaceFirst("\\.class$".toRegex(), "")),
                                     outputFile.asByteArray())
            }
        }

        try {
            val scriptClass = classLoader.loadClass("Line$lineNumber")

            val constructorParams = earlierLines.map(EarlierLine::getScriptClass).toTypedArray()
            val constructorArgs = earlierLines.map(EarlierLine::getScriptInstance).toTypedArray()

            val scriptInstanceConstructor = scriptClass.getConstructor(*constructorParams)
            val scriptInstance = try {
                onUserCodeExecuting(true)
                scriptInstanceConstructor.newInstance(*constructorArgs)
            }
            catch (e: Throwable) {
                return LineResult.runtimeError(renderStackTrace(e.cause!!))
            }
            finally {
                onUserCodeExecuting(false)
            }

            val rvField = scriptClass.getDeclaredField(SCRIPT_RESULT_FIELD_NAME).apply { isAccessible = true }
            val rv = rvField.get(scriptInstance)

            earlierLines.add(EarlierLine(line, scriptDescriptor, scriptClass, scriptInstance))

            return LineResult.successful(rv, !state.replSpecific.hasResult)
        }
        catch (e: Throwable) {
            val writer = PrintWriter(System.err)
            classLoader.dumpClasses(writer)
            writer.flush()
            throw e
        }
    }

    fun dumpClasses(out: PrintWriter) {
        classLoader.dumpClasses(out)
    }

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
        private val REPL_LINE_AS_SCRIPT_DEFINITION = object : KotlinScriptDefinition {
            override fun getScriptParameters(scriptDescriptor: ScriptDescriptor): List<ScriptParameter> = emptyList()

            override fun isScript(file: PsiFile): Boolean = StandardScriptDefinition.isScript(file)

            override fun getScriptName(script: KtScript): Name = StandardScriptDefinition.getScriptName(script)
        }

        private fun renderStackTrace(cause: Throwable): String {
            val newTrace = arrayListOf<StackTraceElement>()
            var skip = true
            for ((i, element) in cause.stackTrace.withIndex().reversed()) {
                // All our code happens in the script constructor, and no reflection/native code happens in constructors.
                // So we ignore everything in the stack trace until the first constructor
                if (element.methodName == "<init>") {
                    skip = false
                }
                if (!skip) {
                    newTrace.add(element)
                }
            }

            // throw away last element which contains Line1.kts<init>(Unknown source)
            val resultingTrace = newTrace.reversed().dropLast(1)

            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UsePropertyAccessSyntax")
            (cause as java.lang.Throwable).setStackTrace(resultingTrace.toTypedArray())

            return Throwables.getStackTraceAsString(cause)
        }

        fun compileScript(
                script: KtScript,
                earlierScripts: List<ScriptDescriptor>,
                state: GenerationState,
                errorHandler: CompilationErrorHandler
        ) {
            state.replSpecific.scriptResultFieldName = SCRIPT_RESULT_FIELD_NAME
            state.replSpecific.earlierScriptsForReplInterpreter = earlierScripts.toList()

            state.beforeCompile()
            KotlinCodegenFacade.generatePackage(
                    state,
                    script.getContainingKtFile().packageFqName,
                    setOf(script.getContainingKtFile()),
                    errorHandler
            )
        }
    }
}
