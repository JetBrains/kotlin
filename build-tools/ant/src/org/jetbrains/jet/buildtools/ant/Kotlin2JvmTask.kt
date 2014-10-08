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
import org.apache.tools.ant.types.Reference

import java.io.File
import java.io.File.pathSeparator

import org.jetbrains.jet.cli.jvm.K2JVMCompiler
import org.jetbrains.jet.cli.common.arguments.K2JVMCompilerArguments
import com.intellij.openapi.util.io.FileUtilRt


/**
 * Kotlin bytecode compiler Ant task.
 * <p/>
 * See
 * http://evgeny-goldin.org/javadoc/ant/tutorial-writing-tasks.html
 * http://evgeny-goldin.org/javadoc/ant/develop.html
 * http://svn.apache.org/viewvc/ant/core/trunk/src/main/org/apache/tools/ant/taskdefs/Javac.java?view=markup.
 */
public class  Kotlin2JvmTask : KotlinCompilerBaseTask<K2JVMCompilerArguments>() {
    override val arguments = K2JVMCompilerArguments()
    override val compiler = K2JVMCompiler()

    public var noStdlib: Boolean = false
    public var externalAnnotations: Path? = null
    public var includeRuntime: Boolean = true

    private var compileClasspath: Path? = null

    public fun createExternalAnnotations(): Path {
        if (externalAnnotations == null) {
            externalAnnotations = Path(getProject())
        }
        return externalAnnotations!!.createPath()
    }

    /**
     * Set the classpath to be used for this compilation.
     *
     * @param classpath an Ant Path object containing the compilation classpath.
     */
    public fun setClasspath(classpath: Path) {
        if (this.compileClasspath == null) {
            this.compileClasspath = classpath
        }
        else {
            this.compileClasspath!!.append(classpath)
        }
    }

    /**
     * Adds a reference to a classpath defined elsewhere.
     *
     * @param ref a reference to a classpath.
     */
    public fun setClasspathRef(ref: Reference) {
        if (this.compileClasspath == null) {
            this.compileClasspath = Path(getProject())
        }
        this.compileClasspath!!.createPath().setRefid(ref)
    }

    /**
     * Set the nested {@code <classpath>} to be used for this compilation.
     *
     * @param classpath an Ant Path object containing the compilation classpath.
     */
    public fun addConfiguredClasspath(classpath: Path) {
        setClasspath(classpath)
    }

    override fun fillSpecificArguments() {
        arguments.destination = getPath(output!!)

        val classpath = arrayListOf<String>()
        compileClasspath?.let { classpath.addAll(it.list()) }
        arguments.freeArgs?.forEach {
            val file = File(it)
            if (file.isDirectory() || file.extension != "kt") {
                classpath.add(it)
            }
        }

        arguments.classpath = classpath.join(pathSeparator)

        arguments.annotations = externalAnnotations?.list()?.join(pathSeparator)
        arguments.noStdlib = noStdlib
        arguments.includeRuntime = includeRuntime
    }
}
