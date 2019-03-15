@file:Suppress("unused")

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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.kotlin.dsl.*
import java.io.File

fun Project.configureFormInstrumentation() {
    plugins.matching { it::class.java.canonicalName.startsWith("org.jetbrains.kotlin.gradle.plugin") }.all {
        // When we change the output classes directory, Gradle will automatically configure
        // the test compile tasks to use the instrumented classes. Normally this is fine,
        // however, it causes problems for Kotlin projects:

        // The "internal" modifier can be used to restrict access to the same module.
        // To make it possible to use internal methods from the main source set in test classes,
        // the Kotlin Gradle plugin adds the original output directory of the Java task
        // as "friendly directory" which makes it possible to access internal members
        // of the main module. Also this directory should be available on classpath during compilation

        // This fails when we change the classes dir. The easiest fix is to prepend the
        // classes from the "friendly directory" to the compile classpath.
        val testCompile = tasks.findByName("compileTestKotlin") as AbstractCompile?
        testCompile?.doFirst {
            testCompile.classpath = (testCompile.classpath
                    - mainSourceSet.output.classesDirs
                    + files((mainSourceSet as ExtensionAware).extra.get("classesDirsCopy")))
        }
    }

    val instrumentationClasspathCfg = configurations.create("instrumentationClasspath")

    dependencies {
        instrumentationClasspathCfg(intellijDep()) { includeJars("javac2", "jdom", "asm-all", rootProject = rootProject) }
    }

    afterEvaluate {
        sourceSets.all { sourceSetParam ->
            // This copy will ignore filters, but they are unlikely to be used.
            val classesDirs = (sourceSetParam.output.classesDirs as ConfigurableFileCollection).from as Collection<Any>

            val classesDirsCopy = project.files(classesDirs.toTypedArray()).filter { it.exists() }
            (sourceSetParam as ExtensionAware).extra.set("classesDirsCopy", classesDirsCopy)

            logger.info("Saving old sources dir for project ${project.name}")
            val instrumentedClassesDir = File(project.buildDir, "classes/${sourceSetParam.name}-instrumented")
            (sourceSetParam.output.classesDirs as ConfigurableFileCollection).setFrom(instrumentedClassesDir)
            val instrumentTask =
                project.tasks.create(sourceSetParam.getTaskName("instrument", "classes"), IntelliJInstrumentCodeTask::class.java)
            instrumentTask.apply {
                dependsOn(sourceSetParam.classesTaskName).onlyIf { !classesDirsCopy.isEmpty }
                sourceSet = sourceSetParam
                instrumentationClasspath = instrumentationClasspathCfg
                originalClassesDirs = classesDirsCopy
                output = instrumentedClassesDir
            }

            instrumentTask.outputs.dir(instrumentedClassesDir)
            // Ensure that our task is invoked when the source set is built
            sourceSetParam.compiledBy(instrumentTask)
            @Suppress("UNUSED_EXPRESSION")
            true
        }
    }
}

@CacheableTask
open class IntelliJInstrumentCodeTask : ConventionTask() {
    companion object {
        private const val FILTER_ANNOTATION_REGEXP_CLASS = "com.intellij.ant.ClassFilterAnnotationRegexp"
        private const val LOADER_REF = "java2.loader"
    }

    var sourceSet: SourceSet? = null

    var instrumentationClasspath: Configuration? = null

    @Input
    var originalClassesDirs: FileCollection? = null

    @get:Input
    var instrumentNotNull: Boolean = false

    @get:InputFiles
    val sourceDirs: FileCollection
        get() = project.files(sourceSet!!.allSource.srcDirs.filter { !sourceSet!!.resources.contains(it) && it.exists() })

    @get:OutputDirectory
    lateinit var output: File

    @TaskAction
    fun instrumentClasses() {
        logger.info(
            "input files are: ${originalClassesDirs?.joinToString(
                "; ",
                transform = { "'${it.name}'${if (it.exists()) "" else " (does not exists)"}" })}"
        )
        output.deleteRecursively()
        copyOriginalClasses()

        val classpath = instrumentationClasspath!!

        ant.withGroovyBuilder {
            "taskdef"(
                "name" to "instrumentIdeaExtensions",
                "classpath" to classpath.asPath,
                "loaderref" to LOADER_REF,
                "classname" to "com.intellij.ant.InstrumentIdeaExtensions"
            )
        }

        logger.info("Compiling forms and instrumenting code with nullability preconditions")
        if (instrumentNotNull) {
            prepareNotNullInstrumenting(classpath.asPath)
        }

        instrumentCode(sourceDirs, instrumentNotNull)
    }

    private fun copyOriginalClasses() {
        project.copy {
            from(originalClassesDirs)
            into(output)
        }
    }

    private fun prepareNotNullInstrumenting(classpath: String) {
        ant.withGroovyBuilder {
            "typedef"(
                "name" to "skip",
                "classpath" to classpath,
                "loaderref" to LOADER_REF,
                "classname" to FILTER_ANNOTATION_REGEXP_CLASS
            )
        }
    }

    private fun instrumentCode(srcDirs: FileCollection, instrumentNotNull: Boolean) {
        val headlessOldValue = System.setProperty("java.awt.headless", "true")

        // Instrumentation needs to have access to sources of forms for inclusion
        val depSourceDirectorySets = project.configurations["compile"].dependencies.withType(ProjectDependency::class.java)
            .map { p -> p.dependencyProject.mainSourceSet.allSource.sourceDirectories }
        val instrumentationClasspath =
            depSourceDirectorySets.fold(sourceSet!!.compileClasspath) { acc, v -> acc + v }.asPath.also {
                logger.info("Using following dependency source dirs: $it")
            }

        logger.info("Running instrumentIdeaExtensions with srcdir=${srcDirs.asPath}}, destdir=$output and classpath=$instrumentationClasspath")

        ant.withGroovyBuilder {
            "instrumentIdeaExtensions"(
                "srcdir" to srcDirs.asPath,
                "destdir" to output,
                "classpath" to instrumentationClasspath,
                "includeantruntime" to false,
                "instrumentNotNull" to instrumentNotNull
            ) {
                if (instrumentNotNull) {
                    ant.withGroovyBuilder {
                        "skip"("pattern" to "kotlin/Metadata")
                    }
                }
            }
        }

        if (headlessOldValue != null) {
            System.setProperty("java.awt.headless", headlessOldValue)
        } else {
            System.clearProperty("java.awt.headless")
        }
    }
}