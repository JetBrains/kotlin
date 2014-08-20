/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import org.jetbrains.jet.buildtools.core.Util
import org.jetbrains.jet.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.jet.cli.common.messages.MessageCollectorPlainTextToStream
import org.jetbrains.jet.cli.js.K2JSCompiler
import java.io.File
import org.apache.tools.ant.BuildException
import org.jetbrains.jet.cli.common.ExitCode

/**
 * Kotlin JavaScript compiler Ant task.
 * http://evgeny-goldin.org/javadoc/ant/tutorial-writing-tasks.html
 */
public class Kotlin2JsCompilerTask : Task() {
    public var src: Path? = null
    public var output: File? = null
    public var library: Path? = null
    public var outputPrefix: File? = null
    public var outputPostfix: File? = null
    public var sourceMap: Boolean = false

    /**
     * {@link K2JsArgumentConstants.CALL} (default) if need generate a main function call (main function will be auto detected)
     * {@link K2JsArgumentConstants.NO_CALL} otherwise.
     */
    public var main: String? = null

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

    public fun createLibrary(): Path {
        val libraryPath = library
        if (libraryPath == null) {
            val t = Path(getProject())
            library = t
            return t
        }

        return libraryPath.createPath()
    }

    override fun execute(): Unit {
        val arguments = K2JSCompilerArguments()

        val sourcePaths = src ?: throw BuildException("\"src\" should be specified")
        arguments.freeArgs = Util.getPaths(sourcePaths.list()).toList()

        val outputFile = output ?: throw BuildException("\"output\" should be specified")
        arguments.outputFile = outputFile.canonicalPath

        arguments.outputPrefix = outputPrefix?.canonicalPath
        arguments.outputPostfix = outputPostfix?.canonicalPath

        arguments.main = main
        arguments.sourceMap = sourceMap

        log("Compiling ${arguments.freeArgs} => [${arguments.outputFile}]");

        val compiler = K2JSCompiler()
        val exitCode = compiler.exec(MessageCollectorPlainTextToStream.PLAIN_TEXT_TO_SYSTEM_ERR, arguments)

        if (exitCode != ExitCode.OK) {
            throw BuildException("Compilation finished with exit code $exitCode")
        }
    }
}
