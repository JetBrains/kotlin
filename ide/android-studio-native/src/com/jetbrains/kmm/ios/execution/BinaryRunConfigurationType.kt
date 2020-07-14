/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios.execution

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.icons.AllIcons
import com.jetbrains.kmm.ios.ProjectWorkspace
import com.jetbrains.konan.KonanBundle
import com.jetbrains.mpp.runconfig.BinaryRunConfigurationFactory

class BinaryRunConfigurationType : ConfigurationTypeBase(
    KonanBundle.message("id.runConfiguration"),
    KonanBundle.message("label.applicationName.text"),
    KonanBundle.message("label.applicationDescription.text"),
    AllIcons.RunConfigurations.Application
) {
    init {
        addFactory(BinaryRunConfigurationFactory(this) { ProjectWorkspace.getInstance(it) })
    }
}