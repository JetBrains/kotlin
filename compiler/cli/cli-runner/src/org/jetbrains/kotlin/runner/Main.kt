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
import java.io.FileNotFoundException
import java.net.URL
import java.util.*
import kotlin.system.exitProcess

object Main {
    private val KOTLIN_HOME: File

    init {
        val home = System.getProperty("kotlin.home")
        if (home == null) {
            System.err.println("error: no kotlin.home system property was passed")
            exitProcess(1)
        }
        KOTLIN_HOME = File(home)
    }

    private fun run(args: Array<String>) {
        val classpath = arrayListOf<URL>()
        val compilerClasspath = arrayListOf<URL>()
        var runner: Runner? = null
        var collectingArguments = false
        var collectingExpressions = false
        var needsCompiler = false
        val arguments = arrayListOf<String>()
        val expressions = arrayListOf<String>()
        var noReflect = false

        var i = 0
        while (i < args.size) {
            val arg = args[i]

            fun next(): String {
                if (++i == args.size) {
                    throw RunnerException("argument expected to $arg")
                }
                return args[i]
            }

            if (collectingExpressions) {
                if ("-expression" == arg || "-e" == arg) {
                    expressions.add(next())
                    i++
                    continue
                } else {
                    collectingArguments = true
                }
            }

            if (collectingArguments) {
                arguments.add(arg)
                i++
                continue
            }

            if ("-help" == arg || "-h" == arg) {
                printUsageAndExit()
            }
            else if ("-version" == arg) {
                printVersionAndExit()
            }
            else if ("-classpath" == arg || "-cp" == arg) {
                for (path in next().split(File.pathSeparator).filter(String::isNotEmpty)) {
                    classpath.addPath(path)
                }
            }
            else if ("-compiler-path" == arg) {
                for (path in next().split(File.pathSeparator).filter(String::isNotEmpty)) {
                    compilerClasspath.addPath(path)
                }
            }
            else if ("-expression" == arg || "-e" == arg) {
                expressions.add(next())
                collectingExpressions = true
                needsCompiler = true
            }
            else if ("-no-reflect" == arg) {
                noReflect = true
            }
            else if (arg.startsWith("-")) {
                throw RunnerException("unsupported argument: $arg")
            }
            else if (arg.endsWith(".jar")) {
                runner = JarRunner(arg)
                collectingArguments = true
            }
            else if (arg.endsWith(".kts")) {
                runner = ScriptRunner(arg)
                collectingArguments = true
                needsCompiler = true
            }
            else {
                runner = MainClassRunner(arg)
                collectingArguments = true
            }
            i++
        }

        if (classpath.isEmpty()) {
            classpath.addPath(".")
        }

        classpath.addPath(KOTLIN_HOME.toString() + "/lib/kotlin-stdlib.jar")

        if (!noReflect) {
            classpath.addPath(KOTLIN_HOME.toString() + "/lib/kotlin-reflect.jar")
        }

        if (expressions.isNotEmpty()) {
            runner = ExpressionRunner(expressions)
        } else if (runner == null) {
            runner = ReplRunner()
            needsCompiler = true
        }

        if (needsCompiler && compilerClasspath.isEmpty()) {
            findCompilerJar(this::class.java, KOTLIN_HOME.resolve("lib")).forEach {
                compilerClasspath.add(it.absoluteFile.toURI().toURL())
            }
        }

        runner.run(classpath, arguments, compilerClasspath)
    }

    private fun MutableList<URL>.addPath(path: String) {
        add(File(path).absoluteFile.toURI().toURL())
    }

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            run(args)
        }
        catch (e: RunnerException) {
            System.err.println("error: " + e.message)
            exitProcess(1)
        }
    }

    private fun printUsageAndExit() {
        println("""kotlin: run Kotlin programs, scripts or REPL.

Usage: kotlin <options> <command> <arguments>
where command may be one of:
  foo.Bar                    Runs the 'main' function from the class with the given qualified name
  app.jar                    Runs the given JAR file as 'java -jar' would do
                             (-classpath argument is ignored and no Kotlin runtime is added to the classpath)
""" +
//  script.kts                 Compiles and runs the given script
//  -expression (-e) '2+2'     Evaluates the expression and prints the result
"""and possible options include:
  -classpath (-cp) <path>    Paths where to find user class files
  -Dname=value               Set a system JVM property
  -J<option>                 Pass an option directly to JVM
  -no-reflect                Don't include Kotlin reflection implementation into classpath
  -version                   Display Kotlin version
  -help (-h)                 Print a synopsis of options
""")
        exitProcess(0)
    }

    private fun printVersionAndExit() {
        val version = try {
            Scanner(File(KOTLIN_HOME, "build.txt")).nextLine()
        }
        catch (e: FileNotFoundException) {
            throw RunnerException("no build.txt was found at home=$KOTLIN_HOME")
        }

        println("Kotlin version " + version + " (JRE " + System.getProperty("java.runtime.version") + ")")
        exitProcess(0)
    }
}
