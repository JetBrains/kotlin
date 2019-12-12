/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard

import com.intellij.openapi.util.registry.Registry

object NewProjectWizardService {
    val isEnabled: Boolean
        get() = Registry.`is`("kotlin.experimental.project.wizard", false)
}