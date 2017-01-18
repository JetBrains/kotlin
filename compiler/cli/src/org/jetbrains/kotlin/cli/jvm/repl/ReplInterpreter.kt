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
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.repl.ReplClassLoader
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.script.KotlinScriptDefinition
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
    private val analyzerEngine = GenericReplAnalyzer(environment)

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
                LineResult.Incomplete
            }
            else {
                LineResult.Error.CompileTime(errorHolder.renderedDiagnostics)
            }
        }

        previousIncompleteLines.clear()

        if (syntaxErrorReport.isHasErrors) {
            return LineResult.Error.CompileTime(errorHolder.renderedDiagnostics)
        }

        val analysisResult = analyzerEngine.analyzeReplLine(psiFile, ReplCodeLine(lineNumber, "fake line"))
        AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder)
        val scriptDescriptor = when (analysisResult) {
            is GenericReplAnalyzer.ReplLineAnalysisResult.WithErrors -> return LineResult.Error.CompileTime(errorHolder.renderedDiagnostics)
            is GenericReplAnalyzer.ReplLineAnalysisResult.Successful -> analysisResult.scriptDescriptor
            else -> error("Unexpected result ${analysisResult.javaClass}")
        }

        val state = GenerationState(
                psiFile.project,
                ClassBuilderFactories.binaries(false), 
                analyzerEngine.module,
                analyzerEngine.trace.bindingContext, 
                listOf(psiFile), 
                configuration)

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
                executeUserCode { scriptInstanceConstructor.newInstance(*constructorArgs) }
            }
            catch (e: Throwable) {
                // ignore everything in the stack trace until this constructor call
                return LineResult.Error.Runtime(renderStackTrace(e.cause!!, startFromMethodName = "${scriptClass.name}.<init>"))
            }

            val rvField = scriptClass.getDeclaredField(SCRIPT_RESULT_FIELD_NAME).apply { isAccessible = true }
            val rv: Any? = rvField.get(scriptInstance)

            earlierLines.add(EarlierLine(line, scriptDescriptor, scriptClass, scriptInstance))

            if (!state.replSpecific.hasResult) {
                return LineResult.UnitResult
            }
            val valueAsString: String = try {
                executeUserCode { rv.toString() }
            }
            catch (e: Throwable) {
                return LineResult.Error.Runtime(renderStackTrace(e, startFromMethodName = "java.lang.String.valueOf"))
            }
            return LineResult.ValueResult(valueAsString)
        }
        catch (e: Throwable) {
            val writer = PrintWriter(System.err)
            classLoader.dumpClasses(writer)
            writer.flush()
            throw e
        }
    }

    private fun <T> executeUserCode(body: () -> T): T {
        try {
            onUserCodeExecuting(true)
            return body()
        }
        finally {
            onUserCodeExecuting(false)
        }
    }

    fun dumpClasses(out: PrintWriter) {
        classLoader.dumpClasses(out)
    }

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
        private val REPL_LINE_AS_SCRIPT_DEFINITION = object : KotlinScriptDefinition(Any::class) {
            override val name = "Kotlin REPL"
        }

        private fun renderStackTrace(cause: Throwable, startFromMethodName: String): String {
            val newTrace = arrayListOf<StackTraceElement>()
            var skip = true
            for ((i, element) in cause.stackTrace.withIndex().reversed()) {
                if ("${element.className}.${element.methodName}" == startFromMethodName) {
                    skip = false
                }
                if (!skip) {
                    newTrace.add(element)
                }
            }

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

sealed class LineResult {
    class ValueResult(val valueAsString: String): LineResult()

    object UnitResult: LineResult()
    object Incomplete : LineResult()

    sealed class Error(val errorText: String): LineResult() {
        class Runtime(errorText: String): Error(errorText)
        class CompileTime(errorText: String): Error(errorText)
    }
}
