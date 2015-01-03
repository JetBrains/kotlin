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

import org.apache.tools.ant.Task
import org.apache.tools.ant.types.Path
import org.apache.tools.ant.types.Reference
import java.io.File
import org.apache.tools.ant.BuildException
import org.apache.tools.ant.types.Commandline
import java.io.PrintStream
import org.apache.tools.ant.AntClassLoader
import java.lang.ref.SoftReference
import org.jetbrains.jet.preloading.ClassPreloadingUtils
import java.net.JarURLConnection

object CompilerClassLoaderHolder {
    private var classLoaderRef = SoftReference<ClassLoader?>(null)

    synchronized fun getOrCreateClassLoader(): ClassLoader {
        val cached = classLoaderRef.get()
        if (cached != null) return cached

        val myLoader = javaClass.getClassLoader()
        if (myLoader !is AntClassLoader) return myLoader

        // Find path of kotlin-ant.jar in the filesystem and find kotlin-compiler.jar in the same directory
        val resourcePath = "/" + javaClass.getName().replace('.', '/') + ".class"
        val jarConnection = javaClass.getResource(resourcePath).openConnection() as? JarURLConnection
                            ?: throw UnsupportedOperationException("Kotlin compiler Ant task should be loaded from the JAR file")
        val antTaskJarPath = File(jarConnection.getJarFileURL().toURI())

        val compilerJarPath = File(antTaskJarPath.getParent(), "kotlin-compiler.jar")
        if (!compilerJarPath.exists()) {
            throw IllegalStateException("kotlin-compiler.jar is not found in the directory of Kotlin Ant task")
        }

        val classLoader = ClassPreloadingUtils.preloadClasses(listOf(compilerJarPath), 4096, myLoader, null)
        classLoaderRef = SoftReference(classLoader)

        return classLoader
    }
}

public abstract class KotlinCompilerBaseTask : Task() {
    protected abstract val compilerFqName: String

    protected val args: MutableList<String> = arrayListOf()

    public var src: Path? = null
    public var output: File? = null
    public var nowarn: Boolean = false
    public var verbose: Boolean = false
    public var printVersion: Boolean = false

    public var noStdlib: Boolean = false

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
        args.addAll(sourcePaths.list().map { File(it).canonicalPath })

        output ?: throw BuildException("\"output\" should be specified")

        if (nowarn) args.add("-nowarn")
        if (verbose) args.add("-verbose")
        if (printVersion) args.add("-version")

        args.addAll(additionalArguments.flatMap { it.getParts().toList() })

        fillSpecificArguments()
    }

    final override fun execute() {
        fillArguments()

        val compilerClass = CompilerClassLoaderHolder.getOrCreateClassLoader().loadClass(compilerFqName)
        val compiler = compilerClass.newInstance()
        val exec = compilerClass.getMethod("execFullPathsInMessages", javaClass<PrintStream>(), javaClass<Array<String>>())

        log("Compiling ${src!!.list().toList()} => [${output!!.canonicalPath}]");

        val exitCode = exec(compiler, System.err, args.copyToArray())

        // TODO: support failOnError attribute of javac
        if ((exitCode as Enum<*>).ordinal() != 0) {
            throw BuildException("Compile failed; see the compiler error output for details.")
        }
    }
}
