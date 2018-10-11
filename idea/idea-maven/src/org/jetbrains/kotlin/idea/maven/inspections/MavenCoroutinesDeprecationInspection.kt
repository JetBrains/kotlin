/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.maven.inspections

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.Module
import com.intellij.util.xml.DomFileElement
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder
import com.intellij.util.xml.highlighting.DomElementsInspection
import org.jetbrains.idea.maven.dom.model.MavenDomDependency
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel
import org.jetbrains.idea.maven.model.MavenId
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.kotlin.idea.configuration.getWholeModuleGroup
import org.jetbrains.kotlin.idea.configuration.isLanguageVersionUpdate
import org.jetbrains.kotlin.idea.inspections.ReplaceStringInDocumentFix
import org.jetbrains.kotlin.idea.inspections.migration.DEPRECATED_COROUTINES_LIBRARIES_INFORMATION
import org.jetbrains.kotlin.idea.inspections.migration.DeprecatedForKotlinLibInfo
import org.jetbrains.kotlin.idea.maven.PomFile
import org.jetbrains.kotlin.idea.maven.findDependencies
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.quickfix.migration.MigrationFix

class MavenCoroutinesDeprecationInspection :
    DomElementsInspection<MavenDomProjectModel>(MavenDomProjectModel::class.java), CleanupLocalInspectionTool, MigrationFix {
    override fun isApplicable(migrationInfo: MigrationInfo): Boolean {
        return migrationInfo.isLanguageVersionUpdate(LanguageVersion.KOTLIN_1_2, LanguageVersion.KOTLIN_1_3)
    }

    override fun checkFileElement(domFileElement: DomFileElement<MavenDomProjectModel>?, holder: DomElementAnnotationHolder?) {
        if (domFileElement == null || holder == null) return

        val file = domFileElement.file
        val module = domFileElement.module ?: return
        val manager = MavenProjectsManager.getInstance(module.project) ?: return
        val mavenProject = manager.findProject(module) ?: return

        val pomFile = PomFile.forFileOrNull(file) ?: return

        for (deprecatedLibInfo in DEPRECATED_COROUTINES_LIBRARIES_INFORMATION) {
            if (!checkKotlinVersion(module, deprecatedLibInfo.sinceKotlinLanguageVersion)) {
                // Same result will be for all invocations in this file, so exit
                return
            }

            val libMavenId = MavenId(deprecatedLibInfo.lib.groupId, deprecatedLibInfo.lib.name, null)

            for (dependency in pomFile.findDependencies(libMavenId)) {
                val versionStr = mavenProject.findDependencies(deprecatedLibInfo.lib.groupId, deprecatedLibInfo.lib.name)
                    .map { it.version }
                    .distinct()
                    .singleOrNull()
                if (versionStr != null) {
                    reportDependency(dependency, versionStr, deprecatedLibInfo, holder)
                }
            }

            for (dependency in pomFile.domModel.dependencyManagement.dependencies.findDependencies(libMavenId)) {
                val versionStr = dependency.version?.stringValue
                if (versionStr != null) {
                    reportDependency(dependency, versionStr, deprecatedLibInfo, holder)
                }
            }
        }
    }

    companion object {
        private fun reportDependency(
            dependency: MavenDomDependency,
            versionStr: String,
            deprecatedLibInfo: DeprecatedForKotlinLibInfo,
            holder: DomElementAnnotationHolder
        ) {
            val updatedVersionStr = deprecatedLibInfo.versionUpdater.updateVersion(versionStr)
            if (updatedVersionStr == versionStr) {
                return
            }

            val xmlElement = dependency.version.xmlElement ?: return
            val xmlText = xmlElement.text ?: return

            if (xmlText.contains(updatedVersionStr)) {
                return
            }

            val fixes: Array<LocalQuickFix> = if (xmlText.contains(versionStr)) {
                arrayOf(ReplaceStringInDocumentFix(xmlElement, versionStr, updatedVersionStr))
            } else {
                emptyArray()
            }

            holder.createProblem(
                dependency.version,
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                deprecatedLibInfo.message,
                null,
                *fixes
            )
        }

        private fun checkKotlinVersion(module: Module, languageVersion: LanguageVersion): Boolean {
            val moduleGroup = module.getWholeModuleGroup()
            return moduleGroup.sourceRootModules.any { moduleInGroup ->
                moduleInGroup.languageVersionSettings.languageVersion >= languageVersion
            }
        }
    }
}