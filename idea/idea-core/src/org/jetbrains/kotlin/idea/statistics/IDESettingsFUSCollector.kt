/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.statistics

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.idea.project.NewInferenceForIDEAnalysisComponent

class IDESettingsFUSCollector : ProjectUsagesCollector() {
    override fun getMetrics(project: Project): Set<MetricEvent> {
        val metrics = mutableSetOf<MetricEvent>()

        @Suppress("DEPRECATION")
        val inferenceState = NewInferenceForIDEAnalysisComponent.isEnabledForV1(project)
        metrics.add(MetricEvent("newInference", flagUsage(inferenceState)))

        val scriptingAutoReloadEnabled = KotlinScriptingSettings.getInstance(project).isAutoReloadEnabled
        metrics.add(MetricEvent("scriptingAutoReloadEnabled", flagUsage(scriptingAutoReloadEnabled)))

        return metrics
    }

    private fun flagUsage(enabled: Boolean): FeatureUsageData {
        return FeatureUsageData()
            .addData("enabled", enabled)
            .addData("pluginVersion", KotlinPluginUtil.getPluginVersion())
    }

    override fun getGroupId() = "kotlin.ide.settings"
    override fun getVersion(): Int = 2
}