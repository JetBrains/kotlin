/*
 * Copyright 2010-2017 JetBrains s.r.o.
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


import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.ReplCheckResponse
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.makeSriptBaseName
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.repl.messages.DiagnosticMessageHolder
import org.jetbrains.kotlin.cli.jvm.repl.messages.ReplTerminalDiagnosticMessageHolder
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

open class GenericReplChecker(
        disposable: Disposable,
        val scriptDefinition: KotlinScriptDefinition,
        val compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector,
        protected val stateLock: ReentrantReadWriteLock = ReentrantReadWriteLock()
) {
    internal val environment = run {
        compilerConfiguration.apply {
            add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, scriptDefinition)
            put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)
        }
        KotlinCoreEnvironment.createForProduction(disposable, compilerConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    private val psiFileFactory: PsiFileFactoryImpl = PsiFileFactory.getInstance(environment.project) as PsiFileFactoryImpl

    // "line" - is the unit of evaluation here, could in fact consists of several character lines
    internal class LineState(
            val codeLine: ReplCodeLine,
            val psiFile: KtFile,
            val errorHolder: DiagnosticMessageHolder)

    private var _lineState: LineState? = null

    internal val lineState: LineState? get() = stateLock.read { _lineState }

    fun createDiagnosticHolder() = ReplTerminalDiagnosticMessageHolder()

    fun check(codeLine: ReplCodeLine, generation: Long): ReplCheckResponse {
        stateLock.write {
            val scriptFileName = makeSriptBaseName(codeLine, generation)
            val virtualFile =
                    LightVirtualFile("${scriptFileName}${KotlinParserDefinition.STD_SCRIPT_EXT}", KotlinLanguage.INSTANCE, codeLine.code).apply {
                        charset = CharsetToolkit.UTF8_CHARSET
                    }
            val psiFile: KtFile = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile?
                                  ?: error("Script file not analyzed at line ${codeLine.no}: ${codeLine.code}")

            val errorHolder = createDiagnosticHolder()

            val syntaxErrorReport = AnalyzerWithCompilerReport.reportSyntaxErrors(psiFile, errorHolder)

            if (!syntaxErrorReport.isHasErrors) {
                _lineState = LineState(codeLine, psiFile, errorHolder)
            }

            return when {
                syntaxErrorReport.isHasErrors && syntaxErrorReport.isAllErrorsAtEof -> ReplCheckResponse.Incomplete()
                syntaxErrorReport.isHasErrors -> ReplCheckResponse.Error(errorHolder.renderedDiagnostics)
                else -> ReplCheckResponse.Ok()
            }
        }
    }
}
