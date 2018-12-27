/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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

        tasks.withType<JavaCompile> {
            doLast {
                instrumentClasses(javaInstrumentator.asPath)
            }
        }
    }
}

fun JavaCompile.instrumentClasses(instrumentatorClasspath: String) {
    ant.withGroovyBuilder {
        "taskdef"(
            "name" to "instrumentIdeaExtensions",
            "classpath" to instrumentatorClasspath,
            "loaderref" to "java2.loader",
            "classname" to "com.intellij.ant.InstrumentIdeaExtensions"
        )
    }

    val sourceSet = project.sourceSets.single { it.compileJavaTaskName == name }

    val javaSourceDirectories = sourceSet.allJava.sourceDirectories.filter { it.exists() }

    ant.withGroovyBuilder {
        javaSourceDirectories.forEach { directory ->
            "instrumentIdeaExtensions"(
                "srcdir" to directory,
                "destdir" to destinationDir,
                "classpath" to classpath.asPath,
                "includeantruntime" to false,
                "instrumentNotNull" to true
            )
        }
    }
}