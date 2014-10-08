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

import org.apache.tools.ant.types.Path
import org.jetbrains.jet.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.jet.cli.common.messages.MessageCollectorPlainTextToStream
import org.jetbrains.jet.cli.js.K2JSCompiler
import java.io.File
import org.apache.tools.ant.BuildException
import org.jetbrains.jet.cli.common.ExitCode
import org.jetbrains.jet.config.Services

/**
 * Kotlin JavaScript compiler Ant task.
 * http://evgeny-goldin.org/javadoc/ant/tutorial-writing-tasks.html
 */
public class Kotlin2JsTask : KotlinCompilerBaseTask<K2JSCompilerArguments>() {
    override val arguments = K2JSCompilerArguments()
    override val compiler = K2JSCompiler()

    public var library: Path? = null
    public var outputPrefix: File? = null
    public var outputPostfix: File? = null
    public var sourceMap: Boolean = false

    /**
     * {@link K2JsArgumentConstants.CALL} (default) if need generate a main function call (main function will be auto detected)
     * {@link K2JsArgumentConstants.NO_CALL} otherwise.
     */
    public var main: String? = null

    public fun createLibrary(): Path {
        val libraryPath = library
        if (libraryPath == null) {
            val t = Path(getProject())
            library = t
            return t
        }

        return libraryPath.createPath()
    }

    override fun fillSpecificArguments() {
        arguments.outputFile = getPath(output!!)

        arguments.outputPrefix = outputPrefix?.canonicalPath
        arguments.outputPostfix = outputPostfix?.canonicalPath

        arguments.main = main
        arguments.sourceMap = sourceMap
    }
}
