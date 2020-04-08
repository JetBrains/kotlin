/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.kotlin.idea.core.script.configuration.AbstractScriptConfigurationManager
import org.jetbrains.kotlin.psi.KtFile

interface SpecialScriptConfigurationManagerProvider {
    fun getSpecialScriptConfigurationManager(ktFile: KtFile): AbstractScriptConfigurationManager?

    companion object {
        val SPECIAL_SCRIPT_CONFIGURATION_MANAGER_PROVIDER: ExtensionPointName<SpecialScriptConfigurationManagerProvider> =
            ExtensionPointName.create("org.jetbrains.kotlin.scripting.idea.specialScriptConfigurationManagerProvider")
    }
}