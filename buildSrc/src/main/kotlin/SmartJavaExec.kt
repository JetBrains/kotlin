import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.task

/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// creating class eagerly here: using register causes problems due to quite complicated relationships between these tasks
fun Project.smartJavaExec(configure: JavaExec.() -> Unit) = tasks.creating(JavaExec::class) {
    configure()
    passClasspathInJar()
}

// Moves the classpath into a jar metadata, to shorten the command line length and to avoid hitting the limit on Windows
fun JavaExec.passClasspathInJar() {
    val jarTask = project.task("${name}WriteClassPath", Jar::class) {
        val classpath = classpath
        val main = mainClass.get()
        dependsOn(classpath)
        inputs.files(classpath)
        inputs.property("main", main)

        archiveFileName.set("$main.${this@passClasspathInJar.name}.classpath.container.jar")
        destinationDirectory.set(temporaryDir)

        doFirst {
            val classPathString = classpath.joinToString(" ") {
                it.toURI().toString()
            }
            manifest {
                attributes(
                    mapOf(
                        "Class-Path" to classPathString,
                        "Main-Class" to main
                    )
                )
            }
        }
    }

    dependsOn(jarTask)

    mainClass.set("-jar")
    classpath = project.files()
    args = listOf(jarTask.outputs.files.singleFile.path) + args.orEmpty()
}
