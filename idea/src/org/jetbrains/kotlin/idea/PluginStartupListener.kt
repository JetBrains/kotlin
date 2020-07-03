/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.ide.ApplicationInitializedListener
import com.intellij.openapi.application.PathMacros
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import org.jetbrains.kotlin.utils.PathUtil.kotlinPathsForIdeaPlugin

class PluginStartupListener : ApplicationInitializedListener {

    override fun componentsInitialized() {
        if (isUnitTestMode()) return

        registerPathVariable()
    }

    private fun registerPathVariable() {
        val macros = PathMacros.getInstance()
        macros.setMacro(KOTLIN_BUNDLED_PATH_VARIABLE, kotlinPathsForIdeaPlugin.homePath.path)
    }

    companion object {
        const val KOTLIN_BUNDLED_PATH_VARIABLE = "KOTLIN_BUNDLED"
    }
}