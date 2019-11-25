/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.core.script.configuration.listener

import org.jetbrains.kotlin.psi.KtFile

open class DefaultScriptChangeListener : ScriptChangeListener {
    override fun editorActivated(file: KtFile, updater: ScriptConfigurationUpdater): Boolean {
        updater.ensureUpToDatedConfigurationSuggested(file)
        return true
    }

    override fun documentChanged(file: KtFile, updater: ScriptConfigurationUpdater): Boolean {
        updater.ensureUpToDatedConfigurationSuggested(file)
        return true
    }
}