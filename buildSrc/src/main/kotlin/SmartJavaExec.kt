import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.task

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

fun Project.smartJavaExec(configure: JavaExec.() -> Unit) = task<JavaExec> javaExec@{
    configure()

    val jarTask = project.task("${name}WriteClassPath", Jar::class) {
        val classpath = classpath
        val main = main
        dependsOn(classpath)
        inputs.files(classpath)
        inputs.property("main", main)
        doFirst {
            val classPathString = classpath.joinToString(" ") { project.file(it).toURI().toString() }
            manifest {
                attributes(
                    mapOf(
                        "Class-Path" to classPathString,
                        "Main-Class" to main
                    )
                )
            }
        }
        archiveName = "$main.${this@javaExec.name}.classpath.container.$extension"
        destinationDir = temporaryDir
    }



    dependsOn(jarTask)

    doFirst {
        main = "-jar"

        classpath = project.files()
        val copyArgs = args.orEmpty().toList()
        args(jarTask.outputs.files.singleFile)
        args(copyArgs)

    }
}
