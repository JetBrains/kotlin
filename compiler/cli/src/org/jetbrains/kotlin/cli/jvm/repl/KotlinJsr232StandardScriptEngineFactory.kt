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

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.KotlinVersion
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.script.StandardScriptDefinition
import org.jetbrains.kotlin.utils.PathUtil
import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory

@Suppress("unused") // used in javax.script.ScriptEngineFactory META-INF file
class KotlinJsr232StandardScriptEngineFactory: ScriptEngineFactory {

    override fun getLanguageName(): String = "kotlin"
    override fun getLanguageVersion(): String = KotlinVersion.VERSION
    override fun getEngineName(): String = "kotlin"
    override fun getEngineVersion(): String = KotlinVersion.VERSION
    override fun getExtensions(): List<String> = listOf("kts")
    override fun getMimeTypes(): List<String> = listOf("text/x-kotlin")
    override fun getNames(): List<String> = listOf("kotlin")

    override fun getScriptEngine(): ScriptEngine =
            KotlinJsr232ScriptEngine(
                    Disposer.newDisposable(),
                    this,
                    StandardScriptDefinition,
                    CompilerConfiguration().apply {
                        addJvmClasspathRoots(PathUtil.getJdkClassesRoots())
                        addJvmClasspathRoots(PathUtil.getKotlinPathsForCompiler().let { listOf(it.runtimePath, it.reflectPath) })
                        // TODO: addJvmClasspathRoots(config.classpath)
                        put(CommonConfigurationKeys.MODULE_NAME, "kotlin-script")
                        put(JVMConfigurationKeys.INCLUDE_RUNTIME, true)
                    })

    override fun getOutputStatement(toDisplay: String?): String = "print(\"$toDisplay\")"
    override fun getMethodCallSyntax(obj: String, m: String, vararg args: String): String = "$obj.$m(${args.joinToString()})"

    override fun getProgram(vararg statements: String): String {
        val sep = System.getProperty("line.separator")
        return statements.joinToString(sep) + sep
    }

    override fun getParameter(key: String?): Any? =
            when (key) {
                ScriptEngine.NAME -> engineName
                ScriptEngine.LANGUAGE -> languageName
                ScriptEngine.LANGUAGE_VERSION -> languageVersion
                ScriptEngine.ENGINE -> engineName
                ScriptEngine.ENGINE_VERSION -> engineVersion
                else -> null
            }
}