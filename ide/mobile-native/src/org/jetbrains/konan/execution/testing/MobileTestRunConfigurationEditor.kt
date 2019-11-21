/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution.testing

import com.intellij.openapi.project.Project
import org.jetbrains.konan.execution.MobileBuildConfigurationHelper
import org.jetbrains.konan.execution.MobileRunConfiguration
import org.jetbrains.konan.execution.MobileRunConfigurationEditor

class MobileTestRunConfigurationEditor(project: Project, helper: MobileBuildConfigurationHelper) :
    MobileRunConfigurationEditor(project, helper) {

    override val allowedModuleNames: Array<String> = arrayOf("androidTest", "iosTest")

    override fun applyEditorTo(runConfiguration: MobileRunConfiguration) {
        super.applyEditorTo(runConfiguration)
        (runConfiguration as MobileTestRunConfiguration).recreateTestData() // TODO do this only when module is changed
    }
}