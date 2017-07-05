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

import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_JAVA_STDLIB_JAR
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

private val kotlinCompilerJar: File by lazy {
    // highest prio - explicit property
    System.getProperty("kotlin.compiler.jar")?.let(::File)?.takeIf(File::exists)
    ?: PathUtil.kotlinPathsForIdeaPlugin.compilerPath
    ?: throw FileNotFoundException("Cannot find kotlin compiler jar, set kotlin.compiler.jar property to proper location")
}

private fun scriptCompilationClasspathFromContext(classLoader: ClassLoader): List<File> =
        ( System.getProperty("kotlin.script.classpath")?.split(File.pathSeparator)?.map(::File)
          ?: classpathFromClassloader(classLoader)
        ).let {
            it?.plus(kotlinScriptStandardJars) ?: kotlinScriptStandardJars
        }
        .map { it?.canonicalFile }
        .distinct()
        .mapNotNull { it?.takeIf(File::exists) }


private val kotlinStdlibJar: File? by lazy {
    System.getProperty("kotlin.java.runtime.jar")?.let(::File)?.takeIf(File::exists)
    ?: File(kotlinCompilerJar.parentFile, KOTLIN_JAVA_STDLIB_JAR).takeIf(File::exists)
}

private val kotlinScriptRuntimeJar: File? by lazy {
    System.getProperty("kotlin.script.runtime.jar")?.let(::File)?.takeIf(File::exists)
    ?: File(kotlinCompilerJar.parentFile, KOTLIN_JAVA_SCRIPT_RUNTIME_JAR).takeIf(File::exists)
}

private val kotlinScriptStandardJars by lazy { listOf(kotlinStdlibJar, kotlinScriptRuntimeJar) }
