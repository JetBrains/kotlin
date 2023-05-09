/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

interface LanguageVersionSettingsProvider {

    fun getModuleLanguageVersionSettings(module: ModuleDescriptor): LanguageVersionSettings?

    companion object {
        fun getInstance(project: Project): LanguageVersionSettingsProvider? =
            project.getService(LanguageVersionSettingsProvider::class.java)
    }
}