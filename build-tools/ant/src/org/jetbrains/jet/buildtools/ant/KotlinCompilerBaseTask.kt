/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.buildtools.ant

import org.apache.tools.ant.Task
import org.apache.tools.ant.types.Path
import org.apache.tools.ant.types.Reference
import org.jetbrains.jet.cli.common.messages.MessageCollectorPlainTextToStream
import java.io.File
import org.apache.tools.ant.BuildException
import org.jetbrains.jet.cli.common.ExitCode
import org.jetbrains.jet.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.jet.cli.common.CLICompiler
import org.apache.tools.ant.types.Commandline
import com.sampullara.cli.Args
import java.io.IOException
import org.jetbrains.jet.config

/**
 * {@code file.getCanonicalPath()} convenience wrapper.
 *
 * @param file - file to get its canonical path.
 * @return file's canonical path
 */
fun getPath(file: File): String {
    try {
        return file.getCanonicalPath()
    }
    catch (e: IOException) {
        throw RuntimeException("Failed to resolve canonical file of [$file]: $e", e)
    }
}

/**
 * Base class for Kotlin compiler Ant tasks.
 * http://evgeny-goldin.org/javadoc/ant/tutorial-writing-tasks.html
 */
public abstract class KotlinCompilerBaseTask<T : CommonCompilerArguments> : Task() {
    protected abstract val arguments: T
    protected abstract val compiler: CLICompiler<T>

    public var src: Path? = null
    public var output: File? = null

    public val additionalArguments: MutableList<Commandline.Argument> = arrayListOf()

    public fun createSrc(): Path {
        val srcPath = src
        if (srcPath == null) {
            val t = Path(getProject())
            src = t
            return t
        }

        return srcPath.createPath()
    }

    public fun setSrcRef(ref: Reference) {
        createSrc().setRefid(ref)
    }

    public fun createCompilerArg(): Commandline.Argument {
        val argument = Commandline.Argument()
        additionalArguments.add(argument)
        return argument
    }

    abstract fun fillSpecificArguments()

    private fun fillArguments() {
        val sourcePaths = src ?: throw BuildException("\"src\" should be specified")
        arguments.freeArgs = sourcePaths.list().map { getPath(File(it)) }

        output ?: throw BuildException("\"output\" should be specified")

        val args = additionalArguments.flatMap { it.getParts()!!.toList() }
        try {
            Args.parse(arguments, args.copyToArray())
        }
        catch (e: IllegalArgumentException) {
            throw BuildException(e.getMessage())
        }

        fillSpecificArguments()
    }

    final override fun execute(): Unit {
        fillArguments()

        val outputPath = getPath(output!!)

        log("Compiling ${arguments.freeArgs} => [${outputPath}]");

        val exitCode = compiler.exec(MessageCollectorPlainTextToStream.PLAIN_TEXT_TO_SYSTEM_ERR, config.Services.EMPTY, arguments)

        if (exitCode != ExitCode.OK) {
            throw BuildException("Compilation finished with exit code $exitCode")
        }
    }
}
