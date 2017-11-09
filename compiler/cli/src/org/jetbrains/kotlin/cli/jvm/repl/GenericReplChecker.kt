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
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.repl.messages.ReplTerminalDiagnosticMessageHolder
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import kotlin.concurrent.write

const val KOTLIN_REPL_JVM_TARGET_PROPERTY = "kotlin.repl.jvm.target"

open class GenericReplChecker(
        disposable: Disposable,
        val scriptDefinition: KotlinScriptDefinition,
        val compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector
) : ReplCheckAction {

    internal val environment = run {
        compilerConfiguration.apply {
            add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, scriptDefinition)
            put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)

            if (get(JVMConfigurationKeys.JVM_TARGET) == null) {
                put(JVMConfigurationKeys.JVM_TARGET,
                    System.getProperty(KOTLIN_REPL_JVM_TARGET_PROPERTY)?.let { JvmTarget.fromString(it) }
                    ?: if (getJavaVersion() >= 0x10008) JvmTarget.JVM_1_8 else JvmTarget.JVM_1_6)
            }
        }
        KotlinCoreEnvironment.createForProduction(disposable, compilerConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    private val psiFileFactory: PsiFileFactoryImpl = PsiFileFactory.getInstance(environment.project) as PsiFileFactoryImpl

    internal fun createDiagnosticHolder() = ReplTerminalDiagnosticMessageHolder()

    override fun check(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCheckResult {
        state.lock.write {
            val checkerState = state.asState(GenericReplCheckerState::class.java)
            val scriptFileName = makeScriptBaseName(codeLine)
            val virtualFile =
                    LightVirtualFile("$scriptFileName${KotlinParserDefinition.STD_SCRIPT_EXT}", KotlinLanguage.INSTANCE, StringUtil.convertLineSeparators(codeLine.code)).apply {
                        charset = CharsetToolkit.UTF8_CHARSET
                    }
            val psiFile: KtFile = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile?
                                  ?: error("Script file not analyzed at line ${codeLine.no}: ${codeLine.code}")

            val errorHolder = createDiagnosticHolder()

            val syntaxErrorReport = AnalyzerWithCompilerReport.reportSyntaxErrors(psiFile, errorHolder)

            if (!syntaxErrorReport.isHasErrors) {
                checkerState.lastLineState = GenericReplCheckerState.LineState(codeLine, psiFile, errorHolder)
            }

            return when {
                syntaxErrorReport.isHasErrors && syntaxErrorReport.isAllErrorsAtEof -> ReplCheckResult.Incomplete()
                syntaxErrorReport.isHasErrors -> ReplCheckResult.Error(errorHolder.renderedDiagnostics)
                else -> ReplCheckResult.Ok()
            }
        }
    }
}

// initially taken from libraries/stdlib/src/kotlin/internal/PlatformImplementations.kt
// fixed according to JEP 223 - http://openjdk.java.net/jeps/223
// TODO: consider to place it to some common place
private fun getJavaVersion(): Int {
    val default = 0x10006
    val version = System.getProperty("java.specification.version") ?: return default
    val components = version.split('.')
    return try {
        when (components.size) {
            0 -> default
            1 -> components[0].toInt() * 0x10000
            else -> components[0].toInt() * 0x10000 + components[1].toInt()
        }
    } catch (e: NumberFormatException) {
        default
    }
}
