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

package org.jetbrains.kotlin.ant

import org.apache.tools.ant.BuildException
import org.apache.tools.ant.taskdefs.Execute
import org.apache.tools.ant.taskdefs.Redirector
import org.apache.tools.ant.types.*
import java.io.File.pathSeparator
import java.io.File.separator

class Kotlin2JvmTask : KotlinCompilerBaseTask() {
    override val compilerFqName = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"

    var includeRuntime: Boolean = false
    var moduleName: String? = null

    var noReflect: Boolean = false

    private val cmdl = CommandlineJava()
    var fork: Boolean = false

    private var compileClasspath: Path? = null

    fun setClasspath(classpath: Path) {
        if (compileClasspath == null) {
            compileClasspath = classpath
        }
        else {
            compileClasspath!!.append(classpath)
        }
    }

    fun setClasspathRef(ref: Reference) {
        if (compileClasspath == null) {
            compileClasspath = Path(getProject())
        }
        compileClasspath!!.createPath().refid = ref
    }

    fun addConfiguredClasspath(classpath: Path) {
        setClasspath(classpath)
    }

    override fun fillSpecificArguments() {
        args.add("-d")
        args.add(output!!.canonicalPath)

        compileClasspath?.let {
            args.add("-classpath")
            args.add(it.list().joinToString(pathSeparator))
        }


        if (moduleName == null) {
            moduleName = defaultModuleName
        }

        moduleName?.let {
            args.add("-module-name")
            args.add(moduleName!!)
        }

        if (noStdlib) args.add("-no-stdlib")
        if (noReflect) args.add("-no-reflect")
        if (includeRuntime) args.add("-include-runtime")
    }

    override fun execute() {
        if (!fork)
            super.execute()
        else {
            exec()
        }
    }

    private fun exec() {
        val javaHome = System.getProperty("java.home")
        val javaBin = javaHome + separator + "bin" + separator + "java"
        val redirector = Redirector(this)

        fillArguments()

        val command = ArrayList<String>()
        command.add(javaBin)
        command.addAll(cmdl.vmCommand.arguments) // jvm args
        command.add("-Dorg.jetbrains.kotlin.cliMessageRenderer=FullPath") // same MessageRenderer as non-forking mode
        command.add("-cp")
        command.add(KotlinAntTaskUtil.compilerJar.canonicalPath)
        command.add(compilerFqName)
        command.addAll(args) // compiler args

        // streamHandler: used to handle the input and output streams of the subprocess.
        // watchdog: a watchdog for the subprocess or <code>null</code> to disable a timeout for the subprocess.
        // TODO: support timeout for the subprocess
        val exe = Execute(redirector.createHandler(), null)
        exe.setAntRun(getProject())
        exe.commandline = command.toTypedArray()
        log("Executing command: ${command.joinToString(" ")}", LogLevel.DEBUG.level)
        log("Compiling ${src!!.list().toList()} => [${output!!.canonicalPath}]")
        val exitCode = exe.execute()
        redirector.complete()
        if (failOnError && exitCode != 0) {
            throw BuildException("Compile failed; see the compiler error output for details.")
        }
    }

    fun createJvmarg(): Commandline.Argument {
        return cmdl.createVmArgument()
    }
}
