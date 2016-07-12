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

import org.apache.tools.ant.types.Path
import java.io.File

class Kotlin2JsTask : KotlinCompilerBaseTask() {
    override val compilerFqName = "org.jetbrains.kotlin.cli.js.K2JSCompiler"

    var library: Path? = null
    var outputPrefix: File? = null
    var outputPostfix: File? = null
    var sourceMap: Boolean = false
    var metaInfo: Boolean = false
    var moduleKind: String = "plain"

    /**
     * {@link K2JsArgumentConstants.CALL} (default) if need generate a main function call (main function will be auto detected)
     * {@link K2JsArgumentConstants.NO_CALL} otherwise.
     */
    var main: String? = null

    fun createLibrary(): Path {
        val libraryPath = library
        if (libraryPath == null) {
            val t = Path(getProject())
            library = t
            return t
        }

        return libraryPath.createPath()
    }

    override fun fillSpecificArguments() {
        args.add("-output")
        args.add(output!!.canonicalPath)

        // TODO: write test
        library?.let {
            args.add("-library-files")
            args.add(it.list().joinToString(separator = ",") { File(it).canonicalPath })
        }

        outputPrefix?.let {
            args.add("-output-prefix")
            args.add(it.canonicalPath)
        }

        outputPostfix?.let {
            args.add("-output-postfix")
            args.add(it.canonicalPath)
        }

        main?.let {
            args.add("-main")
            args.add(it)
        }

        if (noStdlib) args.add("-no-stdlib")
        if (sourceMap) args.add("-source-map")
        if (metaInfo) args.add("-meta-info")

        args += listOf("-module-kind", moduleKind)
    }
}
