/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

allprojects {
    afterEvaluate {
        configureJavaInstrumentation()
    }
}

// Hide window of instrumentation tasks
val headlessOldValue: String? = System.setProperty("java.awt.headless", "true")
logger.info("Setting java.awt.headless=true, old value was $headlessOldValue")

/**
 *  Configures instrumentation for all JavaCompile tasks in project
 */
fun Project.configureJavaInstrumentation() {
    if (plugins.hasPlugin("org.gradle.java")) {
        val javaInstrumentator by configurations.creating
        dependencies {
            javaInstrumentator(intellijDep()) {
                includeJars("javac2", "jdom", "asm-all", rootProject = rootProject)
            }
        }

        listOf(mainSourceSet, testSourceSet).forEach { sourceSet ->
             tasks.named<JavaCompile>(sourceSet.compileJavaTaskName) javaCompile@ {
                doLast {
                    instrumentClasses(javaInstrumentator.asPath, this@javaCompile, sourceSet)
                }
            }
        }
    }
}

fun instrumentClasses(
    instrumentatorClasspath: String,
    javaCompile: JavaCompile,
    sourceSet: SourceSet
) {
    javaCompile.ant.withGroovyBuilder {
        "taskdef"(
            "name" to "instrumentIdeaExtensions",
            "classpath" to instrumentatorClasspath,
            "loaderref" to "java2.loader",
            "classname" to "com.intellij.ant.InstrumentIdeaExtensions"
        )
    }

    val javaSourceDirectories = sourceSet.allJava.sourceDirectories.filter { it.exists() }

    javaCompile.ant.withGroovyBuilder {
        javaSourceDirectories.forEach { directory ->
            "instrumentIdeaExtensions"(
                "srcdir" to directory,
                "destdir" to javaCompile.destinationDir,
                "classpath" to javaCompile.classpath.asPath,
                "includeantruntime" to false,
                "instrumentNotNull" to true
            )
        }
    }
}