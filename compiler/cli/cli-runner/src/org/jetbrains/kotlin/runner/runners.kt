/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.runner

import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.Attributes
import java.util.jar.JarFile

class RunnerException(message: String) : RuntimeException(message)

abstract class AbstractRunner : Runner {
    protected abstract val className: String

    protected abstract fun createClassLoader(classpath: List<URL>): ClassLoader

    override fun run(classpath: List<URL>, arguments: List<String>, compilerClasspath: List<URL>) {
        val classLoader = createClassLoader(classpath)

        val mainClass = try {
            classLoader.loadClass(className)
        }
        catch (e: ClassNotFoundException) {
            throw RunnerException("could not find or load main class $className")
        }

        val main = try {
            mainClass.getDeclaredMethod("main", Array<String>::class.java)
        }
        catch (e: NoSuchMethodException) {
            throw RunnerException("'main' method not found in class $className")
        }

        if (!Modifier.isStatic(main.modifiers)) {
            throw RunnerException(
                    "'main' method of class $className is not static. " +
                    "Please ensure that 'main' is either a top level Kotlin function, " +
                    "a member function annotated with @JvmStatic, or a static Java method"
            )
        }

        Thread.currentThread().contextClassLoader = classLoader
        val savedClasspathProperty = System.setProperty("java.class.path", classpath.joinToString(File.pathSeparator))

        try {
            main.invoke(null, arguments.toTypedArray())
        }
        catch (e: IllegalAccessException) {
            throw RunnerException("'main' method of class $className is not public")
        }
        catch (e: InvocationTargetException) {
            throw e.targetException
        }
        finally {
            if (savedClasspathProperty == null) System.clearProperty("java.class.path")
            else System.setProperty("java.class.path", savedClasspathProperty)
        }
    }
}

class MainClassRunner(override val className: String) : AbstractRunner() {
    override fun createClassLoader(classpath: List<URL>): ClassLoader =
            URLClassLoader(classpath.toTypedArray(), null)
}

class JarRunner(private val path: String) : AbstractRunner() {
    override val className: String =
            try {
                val jar = JarFile(path)
                try {
                    jar.manifest.mainAttributes.getValue(Attributes.Name.MAIN_CLASS)
                }
                finally {
                    jar.close()
                }
            }
            catch (e: IOException) {
                throw RunnerException("could not read manifest from " + path + ": " + e.message)
            }
            ?: throw RunnerException("no Main-Class entry found in manifest in $path")

    override fun createClassLoader(classpath: List<URL>): ClassLoader {
        // 'kotlin *.jar' ignores the passed classpath as 'java -jar' does
        // TODO: warn on non-empty classpath?

        return URLClassLoader(arrayOf(File(path).toURI().toURL()), null)
    }
}

abstract class RunnerWithCompiler : Runner {

    fun runCompiler(compilerClasspath: List<URL>, arguments: List<String>) {
        val classLoader =
            if (arguments.isEmpty()) RunnerWithCompiler::class.java.classLoader
            else URLClassLoader(compilerClasspath.toTypedArray(), null)
        val compilerClass = classLoader.loadClass("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
        val mainMethod = compilerClass.getMethod("main", Array<String>::class.java)
        mainMethod.invoke(null, arguments.toTypedArray())
    }
}

private fun MutableList<String>.addClasspathArgIfNeeded(classpath: List<URL>) {
    if (classpath.isNotEmpty()) {
        add("-cp")
        add(classpath.map {
            if (it.protocol == "file") it.path
            else it.toExternalForm()
        }.joinToString(File.pathSeparator))
    }
}

class ReplRunner : RunnerWithCompiler() {
    override fun run(classpath: List<URL>, arguments: List<String>, compilerClasspath: List<URL>) {
        val compilerArgs = ArrayList<String>()
        compilerArgs.addClasspathArgIfNeeded(classpath)
        runCompiler(compilerClasspath, compilerArgs)
    }
}

class ScriptRunner(private val path: String) : RunnerWithCompiler() {
    override fun run(classpath: List<URL>, arguments: List<String>, compilerClasspath: List<URL>) {
        val compilerArgs = ArrayList<String>().apply {
            addClasspathArgIfNeeded(classpath)
            add("-script")
            add(path)
            addAll(arguments)
        }
        runCompiler(compilerClasspath, compilerArgs)
    }
}

class ExpressionRunner(private val code: List<String>) : RunnerWithCompiler() {
    override fun run(classpath: List<URL>, arguments: List<String>, compilerClasspath: List<URL>) {
        val compilerArgs = ArrayList<String>().apply {
            addClasspathArgIfNeeded(classpath)
            code.forEach {
                add("-expression")
                add(it)
            }
            addAll(arguments)
        }
        runCompiler(compilerClasspath, compilerArgs)
    }
}
