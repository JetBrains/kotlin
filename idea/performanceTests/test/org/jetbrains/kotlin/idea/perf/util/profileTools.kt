/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.util

import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.codeInspection.ex.Tools
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager

class ProfileTools {
    companion object {
        internal fun Project.enableAllInspections() {
            InspectionProfileImpl.INIT_INSPECTIONS = true
            val profile = InspectionProfileImpl("all-inspections")
            profile.enableAllTools(this)
            replaceProfile(profile)
        }

        internal fun Project.disableAllInspections() {
            InspectionProfileImpl.INIT_INSPECTIONS = true
            val profile = InspectionProfileImpl("no-inspections")
            profile.disableAllTools(this)
            replaceProfile(profile)
        }

        internal fun Project.initDefaultProfile() {
            val projectInspectionProfileManager = ProjectInspectionProfileManager.getInstance(this)
            projectInspectionProfileManager.forceLoadSchemes()

            val projectProfile = projectInspectionProfileManager.projectProfile ?: error("project has to have non null profile name")
            val profile = projectInspectionProfileManager.getProfile(projectProfile)
            InspectionProfileImpl.INIT_INSPECTIONS = true
            profile.initInspectionTools(this)
            // disable some known `bad` inspections
            profile.disableInspection(this, "SSBasedInspection")

            val enabledTools = profile.getAllEnabledInspectionTools(this)
            check(enabledTools.isNotEmpty()) {
                "project $name has to have at least one enabled inspection in profile"
            }
            InspectionProfileImpl.INIT_INSPECTIONS = false
        }

        internal fun Project.enableSingleInspection(inspectionName: String) {
            InspectionProfileImpl.INIT_INSPECTIONS = true
            val profile = InspectionProfileImpl("$inspectionName-only")
            profile.disableAllTools(this)
            profile.enableTool(inspectionName, this)

            replaceProfile(profile)
        }

        internal fun Project.enableInspections(vararg inspectionNames: String) {
            InspectionProfileImpl.INIT_INSPECTIONS = true
            val profile = InspectionProfileImpl("custom")
            profile.disableAllTools(this)
            inspectionNames.forEach {
                profile.enableTool(it, this)
            }

            replaceProfile(profile)
        }

        internal fun InspectionProfileImpl.disableInspection(project: Project, vararg shortIds: String) {
            shortIds.forEach { shortId ->
                this.getToolsOrNull(shortId, project)?.let { it.isEnabled = false }
            }
        }

        private fun Project.replaceProfile(profile: InspectionProfileImpl) {
            preloadProfileTools(profile, this)
            val manager = InspectionProjectProfileManager.getInstance(this) as ProjectInspectionProfileManager
            manager.addProfile(profile)
            val prev = manager.currentProfile
            manager.setCurrentProfile(profile)
            Disposer.register(this, {
                InspectionProfileImpl.INIT_INSPECTIONS = false
                manager.setCurrentProfile(prev)
                manager.deleteProfile(profile)
            })
        }

        private fun preloadProfileTools(
            profile: InspectionProfileImpl,
            project: Project
        ) {
            // instantiate all tools to avoid extension loading in inconvenient moment
            profile.getAllEnabledInspectionTools(project).forEach { state: Tools -> state.tool.getTool() }
        }
    }
}
