import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withGroovyBuilder

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class InstrumentJava(@Transient val javaInstrumentator: Configuration, @Transient val sourceSet: SourceSet) : Action<Task> {
    private val instrumentatorClasspath: String by lazy {
        javaInstrumentator.asPath
    }

    private val srcDirs: FileCollection by lazy {
        sourceSet.allJava.sourceDirectories
    }

    override fun execute(task: Task) {
        require(task is JavaCompile) { "$task is not of type JavaCompile!" }
        task.doLast {
            val anySrcDir = srcDirs.filter { it.exists() }.firstOrNull()
            if (anySrcDir != null) {
                task.ant.withGroovyBuilder {
                    "taskdef"(
                        "name" to "instrumentIdeaExtensions",
                        "classpath" to instrumentatorClasspath,
                        "loaderref" to "java2.loader",
                        "classname" to "com.intellij.ant.InstrumentIdeaExtensions"
                    )
                }

                task.ant.withGroovyBuilder {
                    "instrumentIdeaExtensions"(
                        "srcdir" to anySrcDir, // No code should actually be compiled because of areJavaClassesCompiled==true in Javac2 - so any src folder will do.
                        "destdir" to task.destinationDirectory.asFile.get(),
                        "classpath" to task.classpath.asPath,
                        "includeantruntime" to false,
                        "instrumentNotNull" to true
                    )
                }
            }
        }
    }
}