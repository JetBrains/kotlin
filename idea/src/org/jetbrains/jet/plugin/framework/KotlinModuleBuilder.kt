/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.framework

import com.intellij.ide.util.projectWizard.JavaModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleWithNameAlreadyExists
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.util.InvalidDataException
import org.jdom.JDOMException
import org.jetbrains.annotations.Nullable
import org.jetbrains.jet.plugin.JetIcons
import org.jetbrains.jet.plugin.project.TargetPlatform
import javax.swing.*
import java.io.IOException
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.ide.util.projectWizard.WizardContext

public class KotlinModuleBuilder(
        val targetPlatform: TargetPlatform, val builderName: String, val builderDescription: String) : JavaModuleBuilder() {
    override fun getBuilderId() = "kotlin.module.builder"
    override fun getName() = builderName
    override fun getPresentableName() = builderName
    override fun getDescription() = builderDescription
    override fun getBigIcon() = JetIcons.KOTLIN_LOGO_24
    override fun getNodeIcon() = JetIcons.SMALL_LOGO
    override fun getGroupName() = KotlinTemplatesFactory.KOTLIN_GROUP_NAME
    override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider) = ModuleWizardStep.EMPTY_ARRAY

    override fun modifySettingsStep(settingsStep: SettingsStep): ModuleWizardStep {
        return KotlinModuleSettingStep(targetPlatform, this, settingsStep)
    }
}
