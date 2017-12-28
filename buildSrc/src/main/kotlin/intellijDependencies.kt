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
import org.gradle.kotlin.dsl.extra
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.kotlin.dsl.DependencyHandlerScope

private fun Project.intellijRepoDir() = File("${project.rootDir.absoluteFile}/buildSrc/prepare-deps/intellij-sdk/build/repo")

fun RepositoryHandler.intellijSdkRepo(project: Project): IvyArtifactRepository = ivy {
    val baseDir = project.intellijRepoDir()
    setUrl(baseDir)
    ivyPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module]Ultimate.ivy.xml")
    ivyPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module].ivy.xml")
    ivyPattern("${baseDir.canonicalPath}/[organisation]/[revision]/intellijUltimate.plugin.[module].ivy.xml")
    ivyPattern("${baseDir.canonicalPath}/[organisation]/[revision]/intellij.plugin.[module].ivy.xml")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module]Ultimate/lib/[artifact](-[classifier]).jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module]/lib/[artifact](-[classifier]).jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/intellijUltimate/plugins/[module]/lib/[artifact](-[classifier]).jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/intellij/plugins/[module]/lib/[artifact](-[classifier]).jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/plugins-[module]/[module]/lib/[artifact](-[classifier]).jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module]/[artifact].jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module]/[artifact](-[revision])(-[classifier]).jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/sources/[artifact]-[revision]-[classifier].[ext]")
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

fun ModuleDependency.includeIntellijCoreJarDependencies(project: Project) =
        includeJars(*(project.rootProject.extra["IntellijCoreDependencies"] as List<String>).toTypedArray(), rootProject = project.rootProject)

fun ModuleDependency.includeIntellijCoreJarDependencies(project: Project, jarsFilterPredicate: (String) -> Boolean) =
        includeJars(*(project.rootProject.extra["IntellijCoreDependencies"] as List<String>).filter { jarsFilterPredicate(it) }.toTypedArray(), rootProject = project.rootProject)

fun Project.isIntellijCommunityAvailable() = !(rootProject.extra["intellijUltimateEnabled"] as Boolean) || rootProject.extra["intellijSeparateSdks"] as Boolean

fun Project.isIntellijUltimateSdkAvailable() = (rootProject.extra["intellijUltimateEnabled"] as Boolean)

fun Project.intellijRootDir() =
        File(intellijRepoDir(), "kotlin.build.custom.deps/${rootProject.extra["versions.intellijSdk"]}/intellij${if (isIntellijCommunityAvailable()) "" else "Ultimate"}")

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