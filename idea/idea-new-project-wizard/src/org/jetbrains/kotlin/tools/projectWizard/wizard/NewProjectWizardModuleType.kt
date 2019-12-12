package org.jetbrains.kotlin.tools.projectWizard.wizard

import com.intellij.openapi.module.ModuleType
import org.jetbrains.kotlin.idea.KotlinIcons
import javax.swing.Icon

class NewProjectWizardModuleType: ModuleType<NewProjectWizardModuleBuilder>(NewProjectWizardModuleBuilder.MODULE_BUILDER_ID) {
    override fun getName(): String = "New Kotlin Project Wizard"
    override fun getDescription(): String = name
    override fun getNodeIcon(isOpened: Boolean): Icon = KotlinIcons.SMALL_LOGO
    override fun createModuleBuilder(): NewProjectWizardModuleBuilder = NewProjectWizardModuleBuilder()
}