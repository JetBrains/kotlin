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

package org.jetbrains.kotlin.idea.inspections.gradle

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.configuration.KotlinWithGradleConfigurator
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.plugins.gradle.model.data.BuildScriptClasspathData
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.groovy.codeInspection.BaseInspectionVisitor
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase

val KOTLIN_PLUGIN_CLASSPATH_MARKER = "${KotlinWithGradleConfigurator.GROUP_ID}:${KotlinWithGradleConfigurator.GRADLE_PLUGIN_ID}:"

abstract class KotlinGradleInspectionVisitor : BaseInspectionVisitor() {
    override fun visitFile(file: GroovyFileBase) {
        if (file == null || !FileUtilRt.extensionEquals(file.name, GradleConstants.EXTENSION)) return

        val fileIndex = ProjectRootManager.getInstance(file.project).fileIndex

        if (!ApplicationManager.getApplication().isUnitTestMode) {
            val module = fileIndex.getModuleForFile(file.virtualFile) ?: return
            if (!KotlinPluginUtil.isGradleModule(module)) return
        }

        if (fileIndex.isExcluded(file.virtualFile)) return

        super.visitFile(file)
    }
}

fun getResolvedKotlinGradleVersion(file: PsiFile) =
        ModuleUtilCore.findModuleForFile(file.virtualFile, file.project)?.let { getResolvedKotlinGradleVersion(it) }

fun getResolvedKotlinGradleVersion(module: Module): String? {
    val projectStructureNode = findGradleProjectStructure(module) ?: return null
    for (moduleData in projectStructureNode.findAll(ProjectKeys.MODULE).filter { it.data.internalName == module.name }) {
        val buildScriptClasspathData = moduleData.node.findAll(BuildScriptClasspathData.KEY).firstOrNull()?.data ?: continue
        val kotlinPluginVersion = findKotlinPluginVersion(buildScriptClasspathData)
        if (kotlinPluginVersion != null) {
            return kotlinPluginVersion
        }
    }

    return null
}

// Gradle path (example): ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-runtime/<version>
private val KOTLIN_PLUGIN_PATH_MARKER = "${KotlinWithGradleConfigurator.GROUP_ID}/${KotlinWithGradleConfigurator.GRADLE_PLUGIN_ID}/"

// Maven local repo path (example): ~/.m2/repository/org/jetbrains/kotlin/kotlin-runtime/<version>
private val KOTLIN_PLUGIN_PATH_MARKER_FOR_MAVEN_LOCAL_REPO =
        "${KotlinWithGradleConfigurator.GROUP_ID.replace('.', '/')}/${KotlinWithGradleConfigurator.GRADLE_PLUGIN_ID}/"

internal fun findKotlinPluginVersion(classpathData: BuildScriptClasspathData): String? {
    for (classPathEntry in classpathData.classpathEntries.asReversed()) {
        for (path in classPathEntry.classesFile) {
            val uniformedPath = path.replace('\\', '/')
            // check / for local maven repo, and '.' for gradle
            if (uniformedPath.contains(KOTLIN_PLUGIN_PATH_MARKER)) {
                val versionSubstring = uniformedPath.substringAfter(KOTLIN_PLUGIN_PATH_MARKER).substringBefore('/', "<error>")
                if (versionSubstring != "<error>") {
                    return versionSubstring
                }
            } else if (uniformedPath.contains(KOTLIN_PLUGIN_PATH_MARKER_FOR_MAVEN_LOCAL_REPO)) {
                val versionSubstring = uniformedPath.substringAfter(KOTLIN_PLUGIN_PATH_MARKER_FOR_MAVEN_LOCAL_REPO).substringBefore('/', "<error>")
                if (versionSubstring != "<error>") {
                    return versionSubstring
                }
            }
        }
    }

    return null
}

class NodeWithData<T>(val node: DataNode<*>, val data: T)

fun <T: Any> DataNode<*>.findAll(key: Key<T>): List<NodeWithData<T>> {
    val nodes = ExternalSystemApiUtil.findAll(this, key)
    return nodes.mapNotNull {
        val data = it.getData(key) ?: return@mapNotNull null
        NodeWithData(it, data)
    }
}

fun findGradleProjectStructure(file: PsiFile) =
        ModuleUtilCore.findModuleForFile(file.virtualFile, file.project)?.let { findGradleProjectStructure(it) }

fun findGradleProjectStructure(module: Module): DataNode<ProjectData>? {
    val externalProjectPath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return null
    val projectInfo = ExternalSystemUtil.getExternalProjectInfo(module.project, GRADLE_SYSTEM_ID, externalProjectPath) ?: return null
    return projectInfo.externalProjectStructure
}
