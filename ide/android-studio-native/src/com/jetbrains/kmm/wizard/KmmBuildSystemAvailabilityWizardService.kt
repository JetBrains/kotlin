/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.wizard

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.core.service.BuildSystemAvailabilityWizardService
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.isGradle

class KmmBuildSystemAvailabilityWizardService : BuildSystemAvailabilityWizardService {
    override fun isAvailable(buildSystemType: BuildSystemType): Boolean = when {
        buildSystemType.isGradle -> isPluginEnabled("org.jetbrains.plugins.gradle")
        else -> false
    }

    private fun isPluginEnabled(@NonNls id: String) =
        PluginManagerCore.getPlugin(PluginId.getId(id))?.isEnabled == true
}