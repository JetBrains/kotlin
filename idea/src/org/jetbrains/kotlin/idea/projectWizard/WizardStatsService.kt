/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.projectWizard

import org.jetbrains.kotlin.idea.statistics.FUSEventGroups
import org.jetbrains.kotlin.idea.statistics.KotlinFUSLogger

interface WizardStats {
    fun toMap(): Map<String, String>
}

data class ProjectCreationStats(
    val projectTemplateName: String,
    val buildSystemType: String
) : WizardStats {
    override fun toMap(): Map<String, String> = mapOf(
        ::projectTemplateName.name to projectTemplateName,
        ::buildSystemType.name to buildSystemType
    )
}

data class UiEditorUsageStats(
    var modulesCreated: Int = 0,
    var modulesRemoved: Int = 0,
    var moduleTemplatesRemoved: Int = 0,
    var moduleTemplatesSet: Int = 0
) : WizardStats {
    override fun toMap(): Map<String, String> = mapOf(
        ::modulesCreated.name to modulesCreated.toString(),
        ::modulesRemoved.name to modulesRemoved.toString(),
        ::moduleTemplatesRemoved.name to moduleTemplatesRemoved.toString(),
        ::moduleTemplatesSet.name to moduleTemplatesSet.toString()
    )
}

private enum class WizardStatsEvent(val text: String) {
    PROJECT_CREATED("Project Created"),
    WIZARD_STATE_CHANGE("New Wizard enabled or disable")
}

object WizardStatsService {
    fun logDataOnProjectGenerated(projectCreationStats: ProjectCreationStats, uiEditorUsageStats: UiEditorUsageStats) {
        val data = projectCreationStats.toMap() + uiEditorUsageStats.toMap()
        KotlinFUSLogger.log(FUSEventGroups.NewWizard, WizardStatsEvent.PROJECT_CREATED.text, data)
    }

    fun logWizardStatusChanged(isEnabled: Boolean) {
        val data = mapOf("enabled" to isEnabled.toString())
        KotlinFUSLogger.log(FUSEventGroups.NewWizard, WizardStatsEvent.WIZARD_STATE_CHANGE.text, data)
    }
}