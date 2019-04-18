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


// usages in build scripts are not tracked properly
@file:Suppress("unused")

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.task
import java.lang.Character.isLowerCase
import java.lang.Character.isUpperCase

fun Project.projectTest(taskName: String = "test", parallel: Boolean = false, body: Test.() -> Unit = {}): Test = getOrCreateTask(taskName) {
    doFirst {
        val commandLineIncludePatterns = (filter as? DefaultTestFilter)?.commandLineIncludePatterns ?: emptySet()
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

            include {
                val path = it.path
                if (it.isDirectory) {
                    classFileNameWithoutExtension.startsWith(path)
                } else {
                    path == classFileName || (path.endsWith(".class") && path.startsWith("$classFileNameWithoutExtension$"))
                }
            }
        }
    }

    doFirst {
        val agent = tasks.findByPath(":test-instrumenter:jar")!!.outputs.files.singleFile

        val args = project.findProperty("kotlin.test.instrumentation.args")?.let { "=$it" }.orEmpty()

        jvmArgs("-javaagent:$agent$args")
    }

    dependsOn(":test-instrumenter:jar")

    jvmArgs(
        "-ea",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-Xmx1600m",
        "-XX:+UseCodeCacheFlushing",
        "-XX:ReservedCodeCacheSize=128m",
        "-Djna.nosys=true"
    )

    maxHeapSize = "1600m"
    systemProperty("idea.is.unit.test", "true")
    systemProperty("idea.home.path", intellijRootDir().canonicalPath)
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    environment("PROJECT_CLASSES_DIRS", testSourceSet.output.classesDirs.asPath)
    environment("PROJECT_BUILD_DIR", buildDir)
    systemProperty("jps.kotlin.home", rootProject.extra["distKotlinHomeDir"]!!)
    systemProperty("kotlin.ni", if (rootProject.hasProperty("newInferenceTests")) "true" else "false")

    //Temporary workaround for "not enough space" on Teamcity agents: should be fixed soon on Teamcity side
    val teamcity = rootProject.findProperty("teamcity") as? Map<*, *>
    (teamcity?.get("teamcity.build.tempDir") as? String)?.let {
        systemProperty("java.io.tmpdir", it)
    }

    if (parallel) {
        maxParallelForks = Math.max(Runtime.getRuntime().availableProcessors() / 2, 1)
    }
    body()
}

private inline fun String.isFirstChar(f: (Char) -> Boolean) = isNotEmpty() && f(first())

inline fun <reified T : Task> Project.getOrCreateTask(taskName: String, body: T.() -> Unit): T =
    (tasks.findByName(taskName)?.let { it as T } ?: task<T>(taskName)).apply { body() }

object TaskUtils {
    fun useAndroidSdk(task: Task) {
        task.useAndroidConfiguration(systemPropertyName = "android.sdk", configName = "androidSdk")
    }

    fun useAndroidJar(task: Task) {
        task.useAndroidConfiguration(systemPropertyName = "android.jar", configName = "androidJar")
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
