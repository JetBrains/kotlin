/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.projectWizard

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.statistics.FUSEventGroups
import org.jetbrains.kotlin.idea.statistics.KotlinFUSLogger

interface WizardStats {
    fun toMap(): Map<String, String>
}

data class ProjectCreationStats(
    val projectTemplateId: String,
    val buildSystemType: String
) : WizardStats {
    override fun toMap(): Map<String, String> = mapOf(
        "project_template" to projectTemplateId,
        "build_system" to buildSystemType
    )
}

data class UiEditorUsageStats(
    var modulesCreated: Int = 0,
    var modulesRemoved: Int = 0,
    var moduleTemplateChanged: Int = 0
) : WizardStats {
    override fun toMap(): Map<String, String> = mapOf(
        "modules_created" to modulesCreated.toString(),
        "modules_removed" to modulesRemoved.toString(),
        "module_template_changed" to moduleTemplateChanged.toString()
    )
}

private enum class WizardStatsEvent(@NonNls val text: String) {
    PROJECT_OPEN_BY_HYPERLINK("wizard_opened_by_hyperlink"),
    PROJECT_CREATED("project_created")
}

object WizardStatsService {
    fun logWizardOpenByHyperlink(templateId: String?) {
        val data = mapOf("template_id" to (templateId ?: "none"))
        log(WizardStatsEvent.PROJECT_OPEN_BY_HYPERLINK, data)
    }

    fun logDataOnProjectGenerated(projectCreationStats: ProjectCreationStats, uiEditorUsageStats: UiEditorUsageStats) {
        val data = projectCreationStats.toMap() + uiEditorUsageStats.toMap()
        log(WizardStatsEvent.PROJECT_CREATED, data)
    }

    private fun log(event: WizardStatsEvent, data: Map<String, String>) {
        KotlinFUSLogger.log(FUSEventGroups.NewWizard, event.text, data)
    }
}