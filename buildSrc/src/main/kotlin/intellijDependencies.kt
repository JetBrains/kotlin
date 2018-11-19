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

@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.*
import java.io.File

private fun Project.intellijRepoDir() = File("${project.rootDir.absoluteFile}/buildSrc/prepare-deps/intellij-sdk/build/repo")

fun RepositoryHandler.intellijSdkRepo(project: Project): IvyArtifactRepository = ivy {
    val baseDir = project.intellijRepoDir()
    val intellijEnforceCommunitySdk = project.getBooleanProperty("intellijEnforceCommunitySdk") == true

    setUrl(baseDir)

    if (!intellijEnforceCommunitySdk) {
        ivyPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module]Ultimate.ivy.xml")
        ivyPattern("${baseDir.canonicalPath}/[organisation]/[revision]/intellijUltimate.plugin.[module].ivy.xml")
        artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module]Ultimate/lib/[artifact](-[classifier]).jar")
        artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/intellijUltimate/plugins/[module]/lib/[artifact](-[classifier]).jar")
    }

    ivyPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module].ivy.xml")
    ivyPattern("${baseDir.canonicalPath}/[organisation]/[revision]/intellij.plugin.[module].ivy.xml")
    ivyPattern("${baseDir.canonicalPath}/[organisation]/[revision]/plugins-[module].ivy.xml")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module]/lib/[artifact](-[classifier]).jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/intellij/plugins/[module]/lib/[artifact](-[classifier]).jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/plugins-[module]/[module]/lib/[artifact](-[classifier]).jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module]/[artifact].jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module]/[artifact](-[revision])(-[classifier]).jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/sources/[artifact]-[revision]-[classifier].[ext]")

    metadataSources {
        ivyDescriptor()
    }
}

fun Project.intellijDep(module: String = "intellij") = "kotlin.build.custom.deps:$module:${rootProject.extra["versions.intellijSdk"]}"

fun Project.intellijCoreDep() = intellijDep("intellij-core")

fun Project.intellijPluginDep(plugin: String) = intellijDep(plugin)

fun Project.intellijUltimateDep() = intellijDep("intellij")

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
    fun getRepositoryRootDir(project: Project): File = with (project.rootProject) {
        return File(intellijRepoDir(), "kotlin.build.custom.deps/${extra["versions.intellijSdk"]}")
    }

    fun getIntellijRootDir(project: Project): File = with (project.rootProject) {
        return File(getRepositoryRootDir(this), "intellij${if (isIntellijCommunityAvailable()) "" else "Ultimate"}")
    }
}

fun ModuleDependency.includeIntellijCoreJarDependencies(project: Project) =
        includeJars(*(project.rootProject.extra["IntellijCoreDependencies"] as List<String>).toTypedArray(), rootProject = project.rootProject)

fun ModuleDependency.includeIntellijCoreJarDependencies(project: Project, jarsFilterPredicate: (String) -> Boolean) =
        includeJars(*(project.rootProject.extra["IntellijCoreDependencies"] as List<String>).filter { jarsFilterPredicate(it) }.toTypedArray(), rootProject = project.rootProject)

fun Project.isIntellijCommunityAvailable() = !(rootProject.extra["intellijUltimateEnabled"] as Boolean) || rootProject.extra["intellijSeparateSdks"] as Boolean

fun Project.isIntellijUltimateSdkAvailable() = (rootProject.extra["intellijUltimateEnabled"] as Boolean)

fun Project.intellijRootDir() = IntellijRootUtils.getIntellijRootDir(project)

fun Project.intellijUltimateRootDir() =
        if (isIntellijUltimateSdkAvailable())
            File(intellijRepoDir(), "kotlin.build.custom.deps/${rootProject.extra["versions.intellijSdk"]}/intellijUltimate")
        else
            throw GradleException("intellij ultimate SDK is not available")

fun DependencyHandlerScope.excludeInAndroidStudio(rootProject: Project, block: DependencyHandlerScope.() -> Unit) {
    if (!rootProject.extra.has("versions.androidStudioRelease")) {
        block()
    }
}

fun Project.runIdeTask(name: String, ideaPluginDir: File, ideaSandboxDir: File, body: JavaExec.() -> Unit): JavaExec {

    return task<JavaExec>(name) {
        val ideaSandboxConfigDir = File(ideaSandboxDir, "config")

        classpath = mainSourceSet.runtimeClasspath

        main = "com.intellij.idea.Main"

        workingDir = File(intellijRootDir(), "bin")

        jvmArgs(
            "-Xmx1250m",
            "-XX:ReservedCodeCacheSize=240m",
            "-XX:+HeapDumpOnOutOfMemoryError",
            "-ea",
            "-Didea.is.internal=true",
            "-Didea.debug.mode=true",
            "-Didea.system.path=$ideaSandboxDir",
            "-Didea.config.path=$ideaSandboxConfigDir",
            "-Dapple.laf.useScreenMenuBar=true",
            "-Dapple.awt.graphics.UseQuartz=true",
            "-Dsun.io.useCanonCaches=false",
            "-Dplugin.path=${ideaPluginDir.absolutePath}"
        )

        if (rootProject.findProperty("versions.androidStudioRelease") != null) {
            jvmArgs("-Didea.platform.prefix=AndroidStudio")
        }

        if (project.hasProperty("noPCE")) {
            jvmArgs("-Didea.ProcessCanceledException=disabled")
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
