/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.statistics

import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import org.jetbrains.kotlin.idea.KotlinPluginUtil


open class KotlinStatisticsTrigger {
    companion object {
        private val context = FeatureUsageData().addData("plugin_version", KotlinPluginUtil.getPluginVersion())

        fun trigger(trigger: KotlinEventTrigger, event: String) {
            FUCounterUsageLogger.getInstance().logEvent(trigger.GROUP_ID, event, context)
        }
    }
}