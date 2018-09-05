/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.maven.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.Project
import com.intellij.util.xml.DomFileElement
import com.intellij.util.xml.GenericDomValue
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder
import com.intellij.util.xml.highlighting.DomElementsInspection
import org.jetbrains.idea.maven.dom.MavenVersionComparable
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel
import org.jetbrains.idea.maven.model.MavenId
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.kotlin.idea.maven.PomFile
import org.jetbrains.kotlin.idea.maven.configuration.KotlinMavenConfigurator
import org.jetbrains.kotlin.utils.PathUtil

class DifferentMavenStdlibVersionInspection : DomElementsInspection<MavenDomProjectModel>(MavenDomProjectModel::class.java) {
    override fun checkFileElement(domFileElement: DomFileElement<MavenDomProjectModel>?, holder: DomElementAnnotationHolder?) {
        if (domFileElement == null || holder == null) {
            return
        }

        val file = domFileElement.file
        val module = domFileElement.module ?: return
        val manager = MavenProjectsManager.getInstance(module.project) ?: return
        val project = manager.findProject(module) ?: return

        val stdlibVersion = project.findDependencies(KotlinMavenConfigurator.GROUP_ID, PathUtil.KOTLIN_JAVA_STDLIB_NAME).map { it.version }.distinct()
        val pluginVersion = project.findPlugin(KotlinMavenConfigurator.GROUP_ID, KotlinMavenConfigurator.MAVEN_PLUGIN_ID)?.version

        if (pluginVersion == null || stdlibVersion.isEmpty() || stdlibVersion.singleOrNull() == pluginVersion) {
            return
        }

        val pomFile = PomFile.forFileOrNull(file) ?: return
        pomFile.findKotlinPlugins().filter { it.version.stringValue != stdlibVersion.singleOrNull() }.forEach { plugin ->
            val fixes = plugin.version.stringValue?.let { version ->
                createFixes(project, plugin.version, stdlibVersion + version)
            } ?: emptyList()

            holder.createProblem(
                plugin.version,
                HighlightSeverity.WARNING,
                "Plugin version (${plugin.version}) is not the same as library version (${stdlibVersion.joinToString(",", "", "")})",
                *fixes.toTypedArray()
            )
        }

        pomFile.findDependencies(MavenId(KotlinMavenConfigurator.GROUP_ID, PathUtil.KOTLIN_JAVA_STDLIB_NAME, null))
            .filter { it.version.stringValue != pluginVersion }
            .forEach { dependency ->
                val fixes = dependency.version.stringValue?.let { version ->
                    createFixes(project, dependency.version, listOf(version, pluginVersion))
                } ?: emptyList()

                holder.createProblem(
                    dependency.version,
                    HighlightSeverity.WARNING,
                    "Plugin version ($pluginVersion) is not the same as library version (${dependency.version})",
                    *fixes.toTypedArray()
                )
            }
    }

    private fun createFixes(project: MavenProject, versionElement: GenericDomValue<*>, versions: List<String>): List<SetVersionQuickFix> {
        val bestVersion = versions.maxBy(::MavenVersionComparable)!!
        if (bestVersion == versionElement.stringValue) {
            return emptyList()
        }

        val properties = project.properties.entries.filter { it.value == bestVersion }.map { "\${${it.key}}" }

        return properties.map { SetVersionQuickFix(versionElement, it, bestVersion) } +
                SetVersionQuickFix(versionElement, bestVersion, null)
    }

    private class SetVersionQuickFix(val versionElement: GenericDomValue<*>, val newVersion: String, val versionResolved: String?) :
        LocalQuickFix {
        override fun getName() =
            if (versionResolved == null) "Change version to $newVersion" else "Change version to $newVersion ($versionResolved)"

        override fun getFamilyName() = "Change version"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            versionElement.value = newVersion
        }
    }
}