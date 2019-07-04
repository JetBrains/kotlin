/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.sampler

import com.intellij.ide.util.projectWizard.AbstractModuleBuilder
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.platform.ProjectTemplate
import org.jetbrains.kotlin.idea.KotlinIcons
import javax.swing.Icon

class SamplerProjectTemplate : ProjectTemplate {
    override fun getName(): String = "Kotlin Sampler"

    override fun getIcon(): Icon = KotlinIcons.SMALL_LOGO

    override fun getDescription(): String = "Create project from samples published on Git-Hub."

    override fun validateSettings(): ValidationInfo? = null

    override fun createModuleBuilder(): AbstractModuleBuilder = KotlinSamplerModuleBuilder()
}