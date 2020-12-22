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
import org.apache.tools.ant.types.Reference
import java.io.File.pathSeparator

class Kotlin2JvmTask : KotlinCompilerBaseTask() {
    override val compilerFqName = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"

    var includeRuntime: Boolean = true
    var moduleName: String? = null

    var noReflect: Boolean = false

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
        if (includeRuntime && output?.extension != "jar") args.add("-include-runtime")
    }
}
