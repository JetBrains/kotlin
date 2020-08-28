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

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.register
import java.io.File

private fun Project.kotlinBuildLocalDependenciesDir(): File =
    (findProperty("kotlin.build.dependencies.dir") as String?)?.let(::File)
        ?: rootProject.gradle.gradleUserHomeDir.resolve("kotlin-build-dependencies")

private fun Project.kotlinBuildLocalRepoDir(): File = kotlinBuildLocalDependenciesDir().resolve("repo")

private fun Project.ideModuleName() = when (IdeVersionConfigurator.currentIde.kind) {
    Ide.Kind.AndroidStudio -> "android-studio-ide"
    Ide.Kind.IntelliJ -> {
        if (kotlinBuildProperties.intellijUltimateEnabled) "ideaIU" else "ideaIC"
    }
}

private fun Project.ideModuleVersion() = when (IdeVersionConfigurator.currentIde.kind) {
    Ide.Kind.AndroidStudio -> rootProject.findProperty("versions.androidStudioBuild")
    Ide.Kind.IntelliJ -> rootProject.findProperty("versions.intellijSdk")
}

fun RepositoryHandler.kotlinBuildLocalRepo(project: Project): IvyArtifactRepository = ivy {
    val baseDir = project.kotlinBuildLocalRepoDir()
    url = baseDir.toURI()

    patternLayout {
        ivy("[organisation]/[module]/[revision]/[module].ivy.xml")
        ivy("[organisation]/[module]/[revision]/ivy/[module].ivy.xml")
        ivy("[organisation]/${project.ideModuleName()}/[revision]/ivy/[module].ivy.xml") // bundled plugins

        artifact("[organisation]/[module]/[revision]/artifacts/lib/[artifact](-[classifier]).[ext]")
        artifact("[organisation]/[module]/[revision]/artifacts/[artifact](-[classifier]).[ext]")
        artifact("[organisation]/${project.ideModuleName()}/[revision]/artifacts/plugins/[module]/lib/[artifact](-[classifier]).[ext]") // bundled plugins
        artifact("[organisation]/sources/[artifact]-[revision](-[classifier]).[ext]")
        artifact("[organisation]/[module]/[revision]/[artifact](-[classifier]).[ext]")
    }

    metadataSources {
        ivyDescriptor()
    }
}

fun Project.intellijDep(module: String? = null) = "kotlin.build:${module ?: ideModuleName()}:${ideModuleVersion()}"

fun Project.intellijCoreDep() = "kotlin.build:intellij-core:${rootProject.extra["versions.intellijSdk"]}"

fun Project.jpsStandalone() = "kotlin.build:jps-standalone:${rootProject.extra["versions.intellijSdk"]}"

fun Project.nodeJSPlugin() = "kotlin.build:NodeJS:${rootProject.extra["versions.idea.NodeJS"]}"

fun Project.androidDxJar() = "org.jetbrains.kotlin:android-dx:${rootProject.extra["versions.androidBuildTools"]}"

fun Project.jpsBuildTest() = "com.jetbrains.intellij.idea:jps-build-test:${rootProject.extra["versions.intellijSdk"]}"

fun Project.kotlinxCollectionsImmutable() = "org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:${rootProject.extra["versions.kotlinx-collections-immutable"]}"

/**
 * Runtime version of annotations that are already in Kotlin stdlib (historically Kotlin has older version of this one).
 *
 * SHOULD NOT BE USED IN COMPILE CLASSPATH!
 *
 * `@NonNull`, `@Nullabe` from `idea/annotations.jar` has `TYPE` target which leads to different types treatment in Kotlin compiler.
 * On the other hand, `idea/annotations.jar` contains org/jetbrains/annotations/Async annations which is required for IDEA debugger.
 *
 * So, we are excluding `annotaions.jar` from all other `kotlin.build` and using this one for runtime only
 * to avoid accidentally including `annotations.jar` by calling `intellijDep()`.
 */
fun Project.intellijRuntimeAnnotations() = "kotlin.build:intellij-runtime-annotations:${rootProject.extra["versions.intellijSdk"]}"

fun Project.intellijPluginDep(plugin: String) = intellijDep(plugin)

fun Project.intellijUltimateDep() = intellijDep("ideaIU")

fun Project.intellijUltimatePluginDep(plugin: String) = intellijDep(plugin)

fun ModuleDependency.includeJars(vararg names: String, rootProject: Project? = null) {
    names.forEach {
        var baseName = it.removeSuffix(".jar")
        if (rootProject != null && rootProject.extra.has("ignore.jar.$baseName")) {
            return@forEach
        }
        if (rootProject != null && rootProject.extra.has("versions.jar.$baseName")) {
            baseName += "-${rootProject.extra["versions.jar.$baseName"]}"
        }
        artifact {
            name = baseName
            type = "jar"
            extension = "jar"
        }
    }
}

// Workaround. Top-level Kotlin function in a default package can't be called from a non-default package
object IntellijRootUtils {
    fun getRepositoryRootDir(project: Project): File = with(project.rootProject) {
        return File(kotlinBuildLocalRepoDir(), "kotlin.build")
    }

    fun getIntellijRootDir(project: Project): File = with(project.rootProject) {
        return File(
            getRepositoryRootDir(this),
            "${ideModuleName()}/${ideModuleVersion()}/artifacts"
        )
    }
}

fun ModuleDependency.includeIntellijCoreJarDependencies(project: Project) =
    includeJars(*(project.rootProject.extra["IntellijCoreDependencies"] as List<String>).toTypedArray(), rootProject = project.rootProject)

fun ModuleDependency.includeIntellijCoreJarDependencies(project: Project, jarsFilterPredicate: (String) -> Boolean) =
    includeJars(
        *(project.rootProject.extra["IntellijCoreDependencies"] as List<String>).filter { jarsFilterPredicate(it) }.toTypedArray(),
        rootProject = project.rootProject
    )

fun Project.isIntellijCommunityAvailable() =
    !(rootProject.extra["intellijUltimateEnabled"] as Boolean) || rootProject.extra["intellijSeparateSdks"] as Boolean

fun Project.isIntellijUltimateSdkAvailable() = (rootProject.extra["intellijUltimateEnabled"] as Boolean)

fun Project.intellijRootDir() = IntellijRootUtils.getIntellijRootDir(project)

fun Project.intellijUltimateRootDir() =
    if (isIntellijUltimateSdkAvailable())
        File(kotlinBuildLocalRepoDir(), "kotlin.build/ideaIU/${rootProject.extra["versions.intellijSdk"]}/artifacts")
    else
        throw GradleException("intellij ultimate SDK is not available")

fun DependencyHandlerScope.excludeInAndroidStudio(rootProject: Project, block: DependencyHandlerScope.() -> Unit) {
    if (!rootProject.extra.has("versions.androidStudioRelease")) {
        block()
    }
}

fun Project.runIdeTask(name: String, ideaPluginDir: File, ideaSandboxDir: File, body: JavaExec.() -> Unit): TaskProvider<JavaExec> {

    return tasks.register<JavaExec>(name) {
        val ideaSandboxConfigDir = File(ideaSandboxDir, "config")

        classpath = mainSourceSet.runtimeClasspath

        mainClass.set("com.intellij.idea.Main")

        workingDir = File(intellijRootDir(), "bin")

        jvmArgs(
            "-Xmx1250m",
            "-XX:ReservedCodeCacheSize=240m",
            "-XX:+HeapDumpOnOutOfMemoryError",
            "-ea",
            "-Didea.debug.mode=true",
            "-Didea.system.path=$ideaSandboxDir",
            "-Didea.config.path=$ideaSandboxConfigDir",
            "-Didea.tooling.debug=true",
            "-Dapple.laf.useScreenMenuBar=true",
            "-Dapple.awt.graphics.UseQuartz=true",
            "-Dsun.io.useCanonCaches=false",
            "-Dplugin.path=${ideaPluginDir.absolutePath}"
        )

        if (Platform[201].orHigher() && !isIntellijUltimateSdkAvailable()) {
            jvmArgs("-Didea.platform.prefix=Idea")
        }

        if (rootProject.findProperty("versions.androidStudioRelease") != null) {
            jvmArgs("-Didea.platform.prefix=AndroidStudio")
        }

        if (project.hasProperty("noPCE")) {
            jvmArgs("-Didea.ProcessCanceledException=disabled")
        }

        jvmArgs("-Didea.is.internal=${project.findProperty("idea.is.internal") ?: true}")

        project.findProperty("idea.args")?.let { arguments ->
            jvmArgs(arguments.toString().split(" "))
        }

        args()

        doFirst {
            val disabledPluginsFile = File(ideaSandboxConfigDir, "disabled_plugins.txt")
            val disabledPluginsContents = disabledPluginsFile.takeIf { it.isFile }?.readLines()
            val filteredContents = disabledPluginsContents?.filterNot { it.contains("org.jetbrains.kotlin") }
            if (filteredContents != null && filteredContents.size != disabledPluginsContents.size) {
                with(disabledPluginsFile.printWriter()) {
                    filteredContents.forEach(this::println)
                }
            }
        }

        body()
    }
}
