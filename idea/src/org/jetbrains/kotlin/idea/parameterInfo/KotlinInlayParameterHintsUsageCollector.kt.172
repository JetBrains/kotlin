/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hints.InlayParameterHintsExtension
import com.intellij.internal.statistic.UsagesCollector
import com.intellij.internal.statistic.beans.GroupDescriptor
import com.intellij.internal.statistic.beans.UsageDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.util.compat.statistic.getBooleanUsage

class KotlinInlayParameterHintsUsageCollector : UsagesCollector() {
    override fun getGroupId(): GroupDescriptor = GroupDescriptor.create(GROUP_ID)

    override fun getUsages(): Set<UsageDescriptor> {
        val provider = InlayParameterHintsExtension.forLanguage(KotlinLanguage.INSTANCE) ?: return emptySet()

        return provider.supportedOptions.mapTo(LinkedHashSet()) {
            getBooleanUsage(it.id, it.get())
        }
    }

    companion object {
        private const val GROUP_ID = "kotlin.hints.inlay"
    }
}