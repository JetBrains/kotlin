/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import org.jetbrains.kotlin.idea.core.script.configuration.listener.DefaultScriptChangeListener
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptConfigurationUpdater
import org.jetbrains.kotlin.psi.KtFile

class GradleScriptListener : DefaultScriptChangeListener() {
    private val listenGradleRelatedFiles = false // todo

    override fun editorActivated(file: KtFile, updater: ScriptConfigurationUpdater): Boolean {
        if (!isGradleKotlinScript(file.virtualFile)) return false
        if (listenGradleRelatedFiles) {
            updater.ensureUpToDatedConfigurationSuggested(file)
            // todo: force reload if related files changed
        } else {
            // configuration will be reloaded after editor activation, even it is already up-to-date
            // this is required for Gradle scripts, since it's classpath may depend on other files (`.properties` for example)
            updater.forceConfigurationReload(file)
        }

        return true
    }
}