/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


// usages in build scripts are not tracked properly
@file:Suppress("unused")

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.project
import java.io.File
import java.lang.Character.isLowerCase
import java.lang.Character.isUpperCase
import java.nio.file.Files
import java.nio.file.Path

val kotlinGradlePluginAndItsRequired = arrayOf(
    ":kotlin-allopen",
    ":kotlin-noarg",
    ":kotlin-sam-with-receiver",
    ":kotlin-android-extensions",
    ":kotlin-parcelize-compiler",
    ":kotlin-build-common",
    ":kotlin-compiler-embeddable",
    ":native:kotlin-native-utils",
    ":kotlin-util-klib",
    ":kotlin-util-io",
    ":kotlin-compiler-runner",
    ":kotlin-daemon-embeddable",
    ":kotlin-daemon-client",
    ":kotlin-gradle-plugin-api",
    ":kotlin-gradle-plugin",
    ":kotlin-gradle-plugin-model",
    ":kotlin-reflect",
    ":kotlin-annotation-processing-gradle",
    ":kotlin-test",
    ":kotlin-gradle-subplugin-example",
    ":kotlin-stdlib-common",
    ":kotlin-stdlib",
    ":kotlin-stdlib-jdk8",
    ":kotlin-stdlib-js",
    ":examples:annotation-processor-example",
    ":kotlin-script-runtime",
    ":kotlin-scripting-common",
    ":kotlin-scripting-jvm",
    ":kotlin-scripting-compiler-embeddable",
    ":kotlin-scripting-compiler-impl-embeddable",
    ":kotlin-test-js-runner",
    ":native:kotlin-klib-commonizer-embeddable",
    ":native:kotlin-klib-commonizer-api"
)

fun Task.dependsOnKotlinGradlePluginInstall() {
    kotlinGradlePluginAndItsRequired.forEach {
        dependsOn("${it}:install")
    }
}

fun Task.dependsOnKotlinGradlePluginPublish() {
    kotlinGradlePluginAndItsRequired.forEach {
        project.rootProject.tasks.findByPath("${it}:publish")?.let { task ->
            dependsOn(task)
        }
    }
}

/**
 * @param parallel is redundant if @param jUnit5Enabled is true, because
 *   JUnit5 supports parallel test execution by itself, without gradle help
 */
fun Project.projectTest(
    taskName: String = "test",
    parallel: Boolean = false,
    shortenTempRootName: Boolean = false,
    jUnit5Enabled: Boolean = false,
    body: Test.() -> Unit = {}
): TaskProvider<Test> = getOrCreateTask(taskName) {
    doFirst {
        val commandLineIncludePatterns = (filter as? DefaultTestFilter)?.commandLineIncludePatterns ?: mutableSetOf()
        val patterns = filter.includePatterns + commandLineIncludePatterns
        if (patterns.isEmpty() || patterns.any { '*' in it }) return@doFirst
        patterns.forEach { pattern ->
            var isClassPattern = false
            val maybeMethodName = pattern.substringAfterLast('.')
            val maybeClassFqName = if (maybeMethodName.isFirstChar(::isLowerCase)) {
                pattern.substringBeforeLast('.')
            } else {
                isClassPattern = true
                pattern
            }

            if (!maybeClassFqName.substringAfterLast('.').isFirstChar(::isUpperCase)) {
                return@forEach
            }

            val classFileNameWithoutExtension = maybeClassFqName.replace('.', '/')
            val classFileName = "$classFileNameWithoutExtension.class"

            if (isClassPattern) {
                val innerClassPattern = "$pattern$*"
                if (pattern in commandLineIncludePatterns) {
                    commandLineIncludePatterns.add(innerClassPattern)
                    (filter as? DefaultTestFilter)?.setCommandLineIncludePatterns(commandLineIncludePatterns)
                } else {
                    filter.includePatterns.add(innerClassPattern)
                }
            }

            val parentNames = if (jUnit5Enabled) {
                /*
                 * If we run test from inner test class with junit 5 we need
                 *   to include all containing classes of our class
                 */
                val nestedNames = classFileNameWithoutExtension.split("$")
                mutableListOf(nestedNames.first()).also {
                    for (s in nestedNames.subList(1, nestedNames.size)) {
                        it += "${it.last()}\$$s"
                    }
                }
            } else emptyList()

            include { treeElement ->
                val path = treeElement.path
                if (treeElement.isDirectory) {
                    classFileNameWithoutExtension.startsWith(path)
                } else {
                    if (jUnit5Enabled) {
                        path == classFileName || (path.endsWith(".class") && parentNames.any { path.startsWith(it) })
                    } else {
                        path == classFileName || (path.endsWith(".class") && path.startsWith("$classFileNameWithoutExtension$"))
                    }
                }
            }
        }
    }

    if (project.findProperty("kotlin.test.instrumentation.disable")?.toString()?.toBoolean() != true) {
        doFirst {
            val agent = tasks.findByPath(":test-instrumenter:jar")!!.outputs.files.singleFile
            val args = project.findProperty("kotlin.test.instrumentation.args")?.let { "=$it" }.orEmpty()
            jvmArgs("-javaagent:$agent$args")
        }
        dependsOn(":test-instrumenter:jar")
    }

    jvmArgs(
        "-ea",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:+UseCodeCacheFlushing",
        "-XX:ReservedCodeCacheSize=256m",
        "-Djna.nosys=true"
    )

    maxHeapSize = "1600m"
    systemProperty("idea.is.unit.test", "true")
    systemProperty("idea.home.path", intellijRootDir().canonicalPath)
    systemProperty("java.awt.headless", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    environment("PROJECT_CLASSES_DIRS", testSourceSet.output.classesDirs.asPath)
    environment("PROJECT_BUILD_DIR", buildDir)
    systemProperty("jps.kotlin.home", rootProject.extra["distKotlinHomeDir"]!!)
    systemProperty("kotlin.ni", if (rootProject.hasProperty("newInferenceTests")) "true" else "false")
    systemProperty("org.jetbrains.kotlin.skip.muted.tests", if (rootProject.hasProperty("skipMutedTests")) "true" else "false")

    if (Platform[202].orHigher()) {
        systemProperty("idea.ignore.disabled.plugins", "true")
    }

    var subProjectTempRoot: Path? = null
    doFirst {
        val teamcity = rootProject.findProperty("teamcity") as? Map<*, *>
        val systemTempRoot =
            // TC by default doesn't switch `teamcity.build.tempDir` to 'java.io.tmpdir' so it could cause to wasted disk space
            // Should be fixed soon on Teamcity side
            (teamcity?.get("teamcity.build.tempDir") as? String)
                ?: System.getProperty("java.io.tmpdir")
        systemTempRoot.let {
            val prefix = (project.name + "Project_" + taskName + "_").takeUnless { shortenTempRootName }
            subProjectTempRoot = Files.createTempDirectory(File(systemTempRoot).toPath(), prefix)
            systemProperty("java.io.tmpdir", subProjectTempRoot.toString())
        }
    }

    doLast {
        subProjectTempRoot?.let {
            try {
                delete(it)
            } catch (e: Exception) {
                project.logger.warn("Can't delete test temp root folder $it", e.printStackTrace())
            }
        }
    }

    if (parallel && !jUnit5Enabled) {
        maxParallelForks =
            project.findProperty("kotlin.test.maxParallelForks")?.toString()?.toInt()
                ?: (Runtime.getRuntime().availableProcessors() / if (kotlinBuildProperties.isTeamcityBuild) 2 else 4).coerceAtLeast(1)
    }
    body()
}

private inline fun String.isFirstChar(f: (Char) -> Boolean) = isNotEmpty() && f(first())

inline fun <reified T : Task> Project.getOrCreateTask(taskName: String, noinline body: T.() -> Unit): TaskProvider<T> =
    if (tasks.names.contains(taskName)) tasks.named(taskName, T::class.java).apply { configure(body) }
    else tasks.register(taskName, T::class.java, body)

object TaskUtils {
    fun useAndroidSdk(task: Task) {
        task.useAndroidConfiguration(systemPropertyName = "android.sdk", configName = "androidSdk")
    }

    fun useAndroidJar(task: Task) {
        task.useAndroidConfiguration(systemPropertyName = "android.jar", configName = "androidJar")
    }

    fun useAndroidEmulator(task: Task) {
        task.useAndroidConfiguration(systemPropertyName = "android.sdk", configName = "androidEmulator")
    }
}

private fun Task.useAndroidConfiguration(systemPropertyName: String, configName: String) {
    val configuration = with(project) {
        configurations.getOrCreate(configName)
            .also {
                dependencies.add(
                    configName,
                    dependencies.project(":dependencies:android-sdk", configuration = configName)
                )
            }
    }

    dependsOn(configuration)

    if (this is Test) {
        doFirst {
            systemProperty(systemPropertyName, configuration.singleFile.canonicalPath)
        }
    }
}

fun Task.useAndroidSdk() {
    TaskUtils.useAndroidSdk(this)
}

fun Task.useAndroidJar() {
    TaskUtils.useAndroidJar(this)
}
