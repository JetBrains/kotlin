/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.projectWizard

import com.intellij.ide.util.PropertiesComponent

object NewProjectWizardService {
    private const val optionName = "kotlin.experimental.project.wizard"
    private const val enabledByDefault = false

    var isEnabled
        get() = PropertiesComponent.getInstance().getBoolean(optionName, enabledByDefault)
        set(value) {
            if (value != isEnabled) {
                WizardStatsService.logWizardStatusChanged(isEnabled = value)
            }
            PropertiesComponent.getInstance().setValue(optionName, value, enabledByDefault)
        }
}