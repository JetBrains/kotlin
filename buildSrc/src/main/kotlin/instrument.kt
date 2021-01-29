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
            val originalClassesDirs = files((mainSourceSet as ExtensionAware).extra.get("classesDirsCopy"))

            testCompile.classpath = (testCompile.classpath
                    - mainSourceSet.output.classesDirs
                    + originalClassesDirs)

            // Since Kotlin 1.3.60, the friend paths available to the test compile task are calculated as the main source set's
            // output.classesDirs. Since the classesDirs are excluded from the classpath (replaced by the originalClassesDirs),
            // in order to be able to access the internals of 'main', tests need to receive the original classes dirs as a
            // -Xfriend-paths compiler argument as well.
            fun addFreeCompilerArgs(kotlinCompileTask: AbstractCompile, vararg args: String) {
                val getKotlinOptions = kotlinCompileTask::class.java.getMethod("getKotlinOptions")
                val kotlinOptions = getKotlinOptions(kotlinCompileTask)

                val getFreeCompilerArgs = kotlinOptions::class.java.getMethod("getFreeCompilerArgs")
                val freeCompilerArgs = getFreeCompilerArgs(kotlinOptions) as List<*>

                val setFreeCompilerArgs = kotlinOptions::class.java.getMethod("setFreeCompilerArgs", List::class.java)
                setFreeCompilerArgs(kotlinOptions, freeCompilerArgs + args)
            }
            addFreeCompilerArgs(testCompile, "-Xfriend-paths=" + originalClassesDirs.joinToString(",")  { it.absolutePath })
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
                project.tasks.register(sourceSetParam.getTaskName("instrument", "classes"), IntelliJInstrumentCodeTask::class.java) {
                    dependsOn(sourceSetParam.classesTaskName).onlyIf { !classesDirsCopy.isEmpty }
                    sourceSet = sourceSetParam
                    instrumentationClasspathConfiguration = instrumentationClasspathCfg
                    originalClassesDirs = classesDirsCopy
                    output = instrumentedClassesDir
                    outputs.dir(instrumentedClassesDir)
                }

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

    @Transient
    @Internal
    lateinit var instrumentationClasspathConfiguration: Configuration

    @get:Classpath
    val instrumentationClasspath: String by lazy {
        instrumentationClasspathConfiguration.asPath
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    var originalClassesDirs: FileCollection? = null

    @get:Input
    var instrumentNotNull: Boolean = false

    @Transient
    @Internal
    lateinit var sourceSet: SourceSet

    private val compileClasspath by lazy {
        sourceSet.compileClasspath
    }

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sourceDirs: FileCollection by lazy {
        project.files(sourceSet.allSource.srcDirs.filter { !sourceSet.resources.contains(it) && it.exists() })
    }

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

        val classpath = instrumentationClasspath

        ant.withGroovyBuilder {
            "taskdef"(
                "name" to "instrumentIdeaExtensions",
                "classpath" to classpath,
                "loaderref" to LOADER_REF,
                "classname" to "com.intellij.ant.InstrumentIdeaExtensions"
            )
        }

        logger.info("Compiling forms and instrumenting code with nullability preconditions")
        if (instrumentNotNull) {
            prepareNotNullInstrumenting(classpath)
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
            depSourceDirectorySets.fold(compileClasspath) { acc, v -> acc + v }.asPath.also {
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