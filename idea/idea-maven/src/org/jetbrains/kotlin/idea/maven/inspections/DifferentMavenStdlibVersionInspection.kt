/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.maven.KotlinMavenBundle
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

        val stdlibVersion =
            project.findDependencies(KotlinMavenConfigurator.GROUP_ID, PathUtil.KOTLIN_JAVA_STDLIB_NAME).map { it.version }.distinct()
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
                KotlinMavenBundle.message("version.different.plugin.library", plugin.version, stdlibVersion.joinToString(",", "", "")),
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
                    KotlinMavenBundle.message("version.different.plugin.library", pluginVersion, dependency.version),
                    *fixes.toTypedArray()
                )
            }
    }

    private fun createFixes(project: MavenProject, versionElement: GenericDomValue<*>, versions: List<String>): List<SetVersionQuickFix> {
        val bestVersion = versions.maxByOrNull(::MavenVersionComparable)!!
        if (bestVersion == versionElement.stringValue) {
            return emptyList()
        }

        val properties = project.properties.entries.filter { it.value == bestVersion }.map { "\${${it.key}}" }

        return properties.map { SetVersionQuickFix(versionElement, it, bestVersion) } +
                SetVersionQuickFix(versionElement, bestVersion, null)
    }

    private class SetVersionQuickFix(val versionElement: GenericDomValue<*>, val newVersion: String, val versionResolved: String?) :
        LocalQuickFix {
        override fun getName() = when (versionResolved) {
            null -> KotlinMavenBundle.message("fix.set.version.name", newVersion)
            else -> KotlinMavenBundle.message("fix.set.version.name1", newVersion, versionResolved)
        }

        override fun getFamilyName() = KotlinMavenBundle.message("fix.set.version.family")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            versionElement.value = newVersion
        }
    }
}