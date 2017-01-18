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

package org.jetbrains.kotlin.jsr223

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_JAVA_RUNTIME_JAR
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import java.net.URLClassLoader
import javax.script.ScriptContext
import javax.script.ScriptEngine

@Suppress("unused") // used in javax.script.ScriptEngineFactory META-INF file
class KotlinJsr223StandardScriptEngineFactory4Idea : KotlinJsr223JvmScriptEngineFactoryBase() {

    override fun getScriptEngine(): ScriptEngine =
            KotlinJsr223JvmScriptEngine4Idea(
                    Disposer.newDisposable(),
                    this,
                    scriptCompilationClasspathFromContext(Thread.currentThread().contextClassLoader),
                    "kotlin.script.templates.standard.ScriptTemplateWithBindings",
                    { ctx, argTypes -> ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), argTypes ?: emptyArray()) },
                    arrayOf(Map::class)
            )
}

// TODO: some common parts with the code from script-utils, consider placing in a shared lib

private fun URL.toFile() =
        try {
            File(toURI().schemeSpecificPart)
        }
        catch (e: java.net.URISyntaxException) {
            if (protocol != "file") null
            else File(file)
        }

fun classpathFromClassloader(classLoader: ClassLoader): List<File>? =
        generateSequence(classLoader) { it.parent }.toList().flatMap { (it as? URLClassLoader)?.urLs?.mapNotNull(URL::toFile) ?: emptyList() }

private fun File.existsOrNull(): File? = existsAndCheckOrNull { true }
private inline fun File.existsAndCheckOrNull(check: (File.() -> Boolean)): File? = if (exists() && check()) this else null

private val kotlinCompilerJar: File by lazy {
    // highest prio - explicit property
    System.getProperty("kotlin.compiler.jar")?.let(::File)?.existsOrNull()
    ?: PathUtil.getKotlinPathsForIdeaPlugin().compilerPath
    ?: throw FileNotFoundException("Cannot find kotlin compiler jar, set kotlin.compiler.jar property to proper location")
}
private fun <T> Iterable<T>.anyOrNull(predicate: (T) -> Boolean) = if (any(predicate)) this else null

private fun File.matchMaybeVersionedFile(baseName: String) =
        name == baseName ||
        name == baseName.removeSuffix(".jar") || // for classes dirs
        name.startsWith(baseName.removeSuffix(".jar") + "-")

private fun contextClasspath(keyName: String, classLoader: ClassLoader): List<File>? =
        ( classpathFromClassloader(classLoader)?.anyOrNull { it.matchMaybeVersionedFile(keyName) }
        )?.toList()


private fun scriptCompilationClasspathFromContext(classLoader: ClassLoader): List<File> =
        ( System.getProperty("kotlin.script.classpath")?.split(File.pathSeparator)?.map(::File)
          ?: contextClasspath(KOTLIN_JAVA_RUNTIME_JAR, classLoader)
          ?: listOf(kotlinRuntimeJar, kotlinScriptRuntimeJar)
        )
        .map { it?.canonicalFile }
        .distinct()
        .mapNotNull { it?.existsOrNull() }


private val kotlinRuntimeJar: File? by lazy {
    System.getProperty("kotlin.java.runtime.jar")?.let(::File)?.existsOrNull()
    ?: kotlinCompilerJar.let { File(it.parentFile, KOTLIN_JAVA_RUNTIME_JAR) }.existsOrNull()
}

private val kotlinScriptRuntimeJar: File? by lazy {
    System.getProperty("kotlin.script.runtime.jar")?.let(::File)?.existsOrNull()
    ?: kotlinCompilerJar.let { File(it.parentFile, KOTLIN_JAVA_SCRIPT_RUNTIME_JAR) }.existsOrNull()
}
