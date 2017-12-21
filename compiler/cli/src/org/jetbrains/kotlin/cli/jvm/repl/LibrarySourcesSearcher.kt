/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.jvm.repl

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import java.io.File

class LibrarySourcesSearcher(private val compilerConfiguration: CompilerConfiguration) {
    fun findAdditionalSourceJars(classpath: List<File>): List<File> {
        return listOfNotNull(findKotlinStdlibSources(classpath), findJdkSources())
    }

    private fun findJdkSources(): File? {
        var jdkHome = compilerConfiguration[JVMConfigurationKeys.JDK_HOME] ?: return null
        if (jdkHome.name == "jre") {
            jdkHome = jdkHome.parentFile ?: return null
        }

        return File(jdkHome, "src.zip").takeIf { it.exists() } ?: return null
    }

    private fun findKotlinStdlibSources(classpath: List<File>): File? {
        fun File.getClassifier(): String {
            val nameWithoutExtension = this.nameWithoutExtension
            return when {
                nameWithoutExtension.endsWith("-sources") -> "sources"
                nameWithoutExtension.endsWith("-javadoc") -> "javadoc"
                else -> "" // kotlin-stdlib does not have other classifiers for now
            }
        }

        var stdlibSourcesJar: File? = null

        val classpathJars = classpath.filter { it.extension.toLowerCase() == "jar" }

        val sourceJars = classpathJars.filter { it.getClassifier() == "sources" }
        val classesJars = classpathJars.filter { it.getClassifier() == "" }

        classesJars.find { it.name.startsWith("kotlin-stdlib") }?.let { kotlinStdlibJar ->
            if (sourceJars.any { it.name.startsWith("kotlin-stdlib-") }) {
                // There is already sources jar in classpath
                return@let
            }

            val probablySourcesJar = File(kotlinStdlibJar.parentFile, kotlinStdlibJar.nameWithoutExtension + "-sources.jar")
            if (probablySourcesJar.exists()) {
                stdlibSourcesJar = probablySourcesJar
            }
            else {
                // We are probably inside the .gradle dir

                val versionDir = kotlinStdlibJar.parentFile?.parentFile
                val artifactDir = versionDir?.parentFile
                val groupDir = artifactDir?.parentFile

                // .gradle/caches/modules-2/files-2.1/groupName/artifactName/versionName/separateDirsForArtifacts
                val dotGradleDir = groupDir?.parentFile?.parentFile?.parentFile?.parentFile

                if (dotGradleDir != null
                    && dotGradleDir.name == ".gradle"
                    && groupDir.name == "org.jetbrains.kotlin"
                    && artifactDir.name == "kotlin-stdlib"
                        ) {
                    versionDir.listFiles { f: File -> f.isDirectory }
                            .flatMap { (it.listFiles() ?: emptyArray()).asList() }
                            .firstOrNull { it.isFile && it.extension.toLowerCase() == "jar" && it.getClassifier() == "sources" }
                            ?.let { stdlibSourcesJar = it }
                }
            }
        }

        return stdlibSourcesJar
    }
}