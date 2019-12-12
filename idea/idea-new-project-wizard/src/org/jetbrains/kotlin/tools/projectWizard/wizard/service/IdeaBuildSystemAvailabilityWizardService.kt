/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.service

import com.intellij.ide.plugins.PluginManagerCore
import org.jetbrains.kotlin.tools.projectWizard.core.service.BuildSystemAvailabilityWizardService
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.isGradle

class IdeaBuildSystemAvailabilityWizardService : BuildSystemAvailabilityWizardService, IdeaWizardService {
    override fun isAvailable(buildSystemType: BuildSystemType): Boolean = when {
        buildSystemType.isGradle -> !PluginManagerCore.isDisabled("org.jetbrains.plugins.gradle")
        buildSystemType == BuildSystemType.Maven -> !PluginManagerCore.isDisabled("org.jetbrains.idea.maven")
        else -> true
    }
}