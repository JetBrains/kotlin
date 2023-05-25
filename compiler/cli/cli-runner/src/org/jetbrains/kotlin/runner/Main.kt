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

    enum class HowToRun(val argName: String) {
        GUESS("guess"),
        CLASSFILE("classfile"),
        JAR("jar"),
        SCRIPT("script");
        // TODO: consider implementing REPL as well

        companion object {
            val validValues = "${GUESS.argName} (default), ${CLASSFILE.argName}, ${JAR.argName}, ${SCRIPT.argName} (or .<script filename extension>)"

            fun fromArg(name: String): HowToRun? =
                HowToRun.values().find { it.argName == name }
        }
    }

    private fun run(args: Array<String>) {
        val classpath = arrayListOf<URL>()
        val compilerClasspath = arrayListOf<URL>()
        var runner: Runner? = null
        val arguments = arrayListOf<String>()
        val compilerArguments = arrayListOf<String>()
        var noStdLib = false
        var noReflect = false
        var howtorun = HowToRun.GUESS

        fun setRunner(newRunner: Runner) {
            if (runner == null) {
                runner = newRunner
            } else {
                throw AssertionError("conflicting runner settings")
            }
        }

        var i = 0
        while (i < args.size) {
            val arg = args[i]

            fun next(): String {
                if (++i == args.size) {
                    throw RunnerException("argument expected to $arg")
                }
                return args[i]
            }

            fun restAsArguments() {
                arguments.addAll(args.copyOfRange(i+1, args.size))
            }

            when {
                "-help" == arg || "-h" == arg -> {
                    printUsageAndExit()
                }
                "-version" == arg -> {
                    printVersionAndExit()
                }
                "-classpath" == arg || "-cp" == arg -> {
                    for (path in next().split(File.pathSeparator).filter(String::isNotEmpty)) {
                        classpath.addPath(path)
                    }
                }
                "-compiler-path" == arg -> {
                    for (path in next().split(File.pathSeparator).filter(String::isNotEmpty)) {
                        compilerClasspath.addPath(path)
                    }
                }
                "-howtorun" == arg -> {
                    if (howtorun != HowToRun.GUESS) {
                        throw RunnerException("-howtorun is already set to ${howtorun.argName}")
                    }
                    val howToRunArg = next()
                    if (howToRunArg.startsWith(".")) {
                        howtorun = HowToRun.SCRIPT
                        compilerArguments.add("-Xdefault-script-extension=$howToRunArg")
                    } else {
                        howtorun = HowToRun.fromArg(howToRunArg)
                            ?: throw RunnerException("invalid argument to the option -howtorun $howToRunArg, valid arguments are: ${HowToRun.validValues}")
                    }
                }
                "-expression" == arg || "-e" == arg -> {
                    if (howtorun != HowToRun.GUESS && howtorun != HowToRun.SCRIPT) {
                        throw RunnerException("expression evaluation is not compatible with -howtorun argument ${howtorun.argName}")
                    }
                    setRunner(ExpressionRunner(next()))
                    restAsArguments()
                    break
                }
                "-no-stdlib" == arg -> {
                    noStdLib = true
                    compilerArguments.add(arg)
                }
                "-no-reflect" == arg -> {
                    noReflect = true
                    compilerArguments.add(arg)
                }
                arg.startsWith("-X") -> {
                    compilerArguments.add(arg)
                }
                "-language-version" == arg -> {
                    compilerArguments.add(arg)
                    compilerArguments.add(next())
                }
                arg.startsWith("-") -> {
                    throw RunnerException("unknown option: $arg")
                }
                howtorun == HowToRun.JAR || howtorun == HowToRun.GUESS && arg.endsWith(".jar") -> {
                    setRunner(JarRunner(arg))
                    restAsArguments()
                    break
                }
                howtorun == HowToRun.SCRIPT || howtorun == HowToRun.GUESS && arg.endsWith(".kts") -> {
                    setRunner(ScriptRunner(arg))
                    restAsArguments()
                    break
                }
                else -> {
                    val workingDir = File(".")
                    val classFile = File(arg)

                    // Allow running class files with '.class' extension.
                    // In order to infer its fully qualified name, it should be located in the current working directory or a subdirectory of it
                    val className =
                        if (arg.endsWith(".class") && classFile.exists() && classFile.canonicalPath.contains(workingDir.canonicalPath)) {
                            classFile.canonicalFile.toRelativeString(workingDir.canonicalFile)
                                .removeSuffix(".class")
                                .replace(File.separatorChar, '.')
                        } else arg

                    setRunner(MainClassRunner(className))
                    restAsArguments()
                    break
                }
            }
            i++
        }

        if (classpath.isEmpty()) {
            classpath.addPath(".")
        }

        if (!noStdLib) {
            classpath.addPath("$KOTLIN_HOME/lib/kotlin-stdlib.jar")
        }

        if (!noReflect) {
            classpath.addPath("$KOTLIN_HOME/lib/kotlin-reflect.jar")
        }

        if (runner == null) {
            setRunner(ReplRunner())
        }

        if (runner is RunnerWithCompiler && compilerClasspath.isEmpty()) {
            findCompilerJar(this::class.java, KOTLIN_HOME.resolve("lib")).forEach {
                compilerClasspath.add(it.absoluteFile.toURI().toURL())
            }
        }

        runner!!.run(classpath, compilerArguments, arguments, compilerClasspath)
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

Usage: kotlin <options> <command> [<arguments>]
where possible options include:
  -howtorun <value>          How to run the supplied command with arguments, 
                             valid values: ${HowToRun.validValues}
  -classpath (-cp) <path>    Paths where to find user class files
  -Dname=value               Set a system JVM property
  -J<option>                 Pass an option directly to JVM
  -no-stdlib                 Don't include Kotlin standard library into classpath
  -no-reflect                Don't include Kotlin reflection implementation into classpath
  -compiler-path             Kotlin compiler classpath for compiling script or expression or running REPL 
                             If not specified, try to find the compiler in the environment
  -X<flag>[=value]           Pass -X argument to the compiler
  -version                   Display Kotlin version
  -help (-h)                 Print a synopsis of options
and command is interpreted according to the -howtorun option argument 
or, in case of guess, according to the following rules:
  foo.Bar                    Runs the 'main' function from the class with the given qualified name
                             (compiler arguments are ignored) 
  app.jar                    Runs the given JAR file as 'java -jar' would do
                             (compiler arguments are ignored and no Kotlin stdlib is added to the classpath)
  script.kts                 Compiles and runs the given script, passing <arguments> to it
  -expression (-e) '2+2'     Evaluates the expression and prints the result, passing <arguments> to it
  <no command>               Runs Kotlin REPL
arguments are passed to the main function when running class or jar file, and for standard script definitions
as the 'args' parameter when running script or expression
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
