/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.openapi.application.PathMacroContributor
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import org.jetbrains.kotlin.utils.PathUtil

/**
 * Some actions have to be performed before loading and opening any project.
 *
 * E.g. path variables have to be registered in advance as modules could rely on some path variables.
 */
class KotlinPluginMacros : PathMacroContributor {
    override fun registerPathMacros(macros: MutableMap<String, String>, legacyMacros: MutableMap<String, String>) {
        if (!isUnitTestMode()) {
            macros[KOTLIN_BUNDLED_PATH_VARIABLE] = PathUtil.kotlinPathsForIdeaPlugin.homePath.path
        }
    }

    companion object {
        const val KOTLIN_BUNDLED_PATH_VARIABLE = "KOTLIN_BUNDLED"
    }

}
