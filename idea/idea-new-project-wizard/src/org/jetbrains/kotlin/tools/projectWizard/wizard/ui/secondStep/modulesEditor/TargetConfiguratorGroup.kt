/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor

import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.icon
import javax.swing.Icon

interface TargetConfiguratorGroup

interface DisplayableTargetConfiguratorGroup : TargetConfiguratorGroup, DisplayableSettingItem {
    val moduleType: ModuleType
    val icon: Icon
}

interface TargetConfiguratorGroupWithSubItems : TargetConfiguratorGroup {
    val subItems: List<DisplayableSettingItem>
}

data class StepTargetConfiguratorGroup(
    @Nls override val text: String,
    override val moduleType: ModuleType,
    override val subItems: List<DisplayableSettingItem>
) : DisplayableTargetConfiguratorGroup, TargetConfiguratorGroupWithSubItems {
    override val icon: Icon get() = moduleType.icon
}

data class FinalTargetConfiguratorGroup(
    @Nls override val text: String,
    override val moduleType: ModuleType,
    override val icon: Icon,
    override val subItems: List<DisplayableSettingItem>
) : DisplayableTargetConfiguratorGroup, TargetConfiguratorGroupWithSubItems

data class FirstStepTargetConfiguratorGroup(
    override val subItems: List<DisplayableSettingItem>
) : TargetConfiguratorGroup, TargetConfiguratorGroupWithSubItems