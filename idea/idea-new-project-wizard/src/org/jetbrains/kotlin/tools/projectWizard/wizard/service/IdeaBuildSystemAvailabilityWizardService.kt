/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.service

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.core.service.BuildSystemAvailabilityWizardService
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.isGradle

class IdeaBuildSystemAvailabilityWizardService : BuildSystemAvailabilityWizardService, IdeaWizardService {
    override fun isAvailable(buildSystemType: BuildSystemType): Boolean = when {
        buildSystemType.isGradle -> isPluginEnabled("org.jetbrains.plugins.gradle")
        buildSystemType == BuildSystemType.Maven -> isPluginEnabled("org.jetbrains.idea.maven")
        else -> true
    }

    private fun isPluginEnabled(@NonNls id: String) =
        PluginManagerCore.getPlugin(PluginId.getId(id))?.isEnabled == true
}