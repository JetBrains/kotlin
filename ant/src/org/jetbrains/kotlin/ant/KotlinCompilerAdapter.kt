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
import org.apache.tools.ant.MagicNames
import org.apache.tools.ant.Project.MSG_ERR
import org.apache.tools.ant.Project.MSG_WARN
import org.apache.tools.ant.taskdefs.compilers.Javac13
import org.apache.tools.ant.taskdefs.condition.AntVersion
import org.apache.tools.ant.types.Commandline
import org.apache.tools.ant.types.Path
import java.io.File
import java.util.*

class KotlinCompilerAdapter : Javac13() {
    var moduleName: String? = null

    var additionalArguments: MutableList<Commandline.Argument> = ArrayList(0)

    @Suppress("unused") // Used via reflection by Ant
    fun createCompilerArg(): Commandline.Argument {
        val argument = Commandline.Argument()
        additionalArguments.add(argument)
        return argument
    }

    override fun getSupportedFileExtensions(): Array<String> = super.getSupportedFileExtensions() + KOTLIN_EXTENSIONS

    @Throws(BuildException::class)
    override fun execute(): Boolean {
        if (javac.isForkedJavac) {
            javac.log("<withKotlin> task does not yet support the fork mode", MSG_ERR)
            return false
        }

        val javac = javac

        checkAntVersion()

        val kotlinc = Kotlin2JvmTask()
        kotlinc.failOnError = javac.failonerror
        kotlinc.output = javac.destdir

        val classpath = javac.classpath
        if (classpath != null) {
            kotlinc.setClasspath(classpath)
        }

        // We use the provided src dir instead of compileList, because the latter is insane:
        // it is constructed only of sources which are newer than classes with the same name
        kotlinc.src = javac.srcdir

        if (moduleName == null) {
            moduleName = javac.defaultModuleName
        }
        kotlinc.moduleName = moduleName

        kotlinc.additionalArguments.addAll(additionalArguments)

        // Javac13#execute passes everything in compileList to javac, which doesn't recognize .kt files
        val compileListForJavac = filterOutKotlinSources(compileList)

        val hasKotlinFilesInSources = compileListForJavac.size < compileList.size

        if (hasKotlinFilesInSources) {
            kotlinc.execute()
            if (kotlinc.exitCode != 0) {
                // Don't run javac if failOnError = false and there were errors on Kotlin sources
                return false
            }
        }
        else {
            // This is needed for addRuntimeToJavacClasspath, where kotlinc arguments will be used.
            kotlinc.fillArguments()
        }

        javac.log("Running javac...")

        compileList = compileListForJavac

        addRuntimeToJavacClasspath(kotlinc)

        return compileList.isEmpty() || super.execute()
    }

    private fun addRuntimeToJavacClasspath(kotlinc: Kotlin2JvmTask) {
        // If "-no-stdlib" (or "-no-reflect") was specified explicitly, probably the user also wanted the javac classpath to not have it
        val addStdlib = "-no-stdlib" !in kotlinc.args
        val addReflect = "-no-reflect" !in kotlinc.args

        if (!addStdlib && !addReflect) return

        if (compileClasspath == null) {
            compileClasspath = Path(getProject())
        }
        if (addStdlib) {
            compileClasspath.add(Path(getProject(), KotlinAntTaskUtil.runtimeJar.absolutePath))
        }
        // "-no-stdlib" implies "-no-reflect", see K2JVMCompiler.Companion.getClasspath
        if (addReflect && addStdlib) {
            compileClasspath.add(Path(getProject(), KotlinAntTaskUtil.reflectJar.absolutePath))
        }
    }

    private fun checkAntVersion() {
        val checkVersion = AntVersion()
        checkVersion.atLeast = "1.8.2"
        if (!checkVersion.eval()) {
            javac.log("<withKotlin> task requires Ant of version at least 1.8.2 to operate reliably. " +
                      "Please upgrade or, as a workaround, make sure you have at least one Java source and " +
                      "the output directory is clean before running this task. " +
                      "You have: " + getProject().getProperty(MagicNames.ANT_VERSION), MSG_WARN)
        }
    }

    companion object {
        private val KOTLIN_EXTENSIONS = Arrays.asList("kt", "kts")

        private fun filterOutKotlinSources(files: Array<File>): Array<File> {
            return files.filterNot {
                for (extension in KOTLIN_EXTENSIONS) {
                    if (it.path.endsWith("." + extension)) return@filterNot true
                }
                false
            }.toTypedArray()
        }
    }
}
