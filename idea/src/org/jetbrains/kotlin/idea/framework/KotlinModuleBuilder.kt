/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.framework

import com.intellij.ide.util.projectWizard.JavaModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.resolve.TargetPlatform

class KotlinModuleBuilder(
        val targetPlatform: TargetPlatform, val builderName: String, val builderDescription: String) : JavaModuleBuilder() {
    override fun getBuilderId() = "kotlin.module.builder"
    override fun getName() = builderName
    override fun getPresentableName() = builderName
    override fun getDescription() = builderDescription
    override fun getNodeIcon() = KotlinIcons.SMALL_LOGO
    override fun getGroupName() = KotlinTemplatesFactory.KOTLIN_GROUP_NAME
    override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider) = ModuleWizardStep.EMPTY_ARRAY

    override fun modifySettingsStep(settingsStep: SettingsStep): ModuleWizardStep {
        return KotlinModuleSettingStep(targetPlatform, this, settingsStep)
    }
}
