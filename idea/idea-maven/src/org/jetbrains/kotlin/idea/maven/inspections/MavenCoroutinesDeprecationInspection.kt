/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.maven.inspections

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.Module
import com.intellij.util.xml.DomFileElement
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder
import com.intellij.util.xml.highlighting.DomElementsInspection
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel
import org.jetbrains.idea.maven.model.MavenId
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.configuration.getWholeModuleGroup
import org.jetbrains.kotlin.idea.inspections.migration.DEPRECATED_COROUTINES_LIBRARIES_INFORMATION
import org.jetbrains.kotlin.idea.maven.PomFile
import org.jetbrains.kotlin.idea.maven.findDependencies
import org.jetbrains.kotlin.idea.project.languageVersionSettings

class MavenCoroutinesDeprecationInspection :
    DomElementsInspection<MavenDomProjectModel>(MavenDomProjectModel::class.java), CleanupLocalInspectionTool {

    override fun checkFileElement(domFileElement: DomFileElement<MavenDomProjectModel>?, holder: DomElementAnnotationHolder?) {
        if (domFileElement == null || holder == null) return

        val file = domFileElement.file
        val module = domFileElement.module ?: return

        val pomFile = PomFile.forFileOrNull(file) ?: return

        for (libInfo in DEPRECATED_COROUTINES_LIBRARIES_INFORMATION) {
            if (!checkKotlinVersion(module, libInfo.sinceKotlinLanguageVersion)) {
                // Same result will be for all invocations in this file, so exit
                return
            }

            val libMavenId = MavenId(libInfo.lib.groupId, libInfo.lib.name, null)

            val moduleDependencies = pomFile.findDependencies(libMavenId)
            val dependencyManagementDependencies = pomFile.domModel.dependencyManagement.dependencies.findDependencies(libMavenId)

            for (dependency in moduleDependencies + dependencyManagementDependencies) {
                val xmlElement = dependency.version.xmlElement
                if (xmlElement != null) {
                    holder.createProblem(
                        dependency.version,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        libInfo.message,
                        null
                    )
                }
            }
        }
    }

    companion object {
        private fun checkKotlinVersion(module: Module, languageVersion: LanguageVersion): Boolean {
            val moduleGroup = module.getWholeModuleGroup()
            return moduleGroup.sourceRootModules.any { moduleInGroup ->
                moduleInGroup.languageVersionSettings.languageVersion >= languageVersion
            }
        }
    }
}