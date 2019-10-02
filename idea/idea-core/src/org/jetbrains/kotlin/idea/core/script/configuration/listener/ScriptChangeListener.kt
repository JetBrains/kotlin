/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.listener

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.idea.core.script.configuration.DefaultScriptConfigurationManager

/**
 * [ScriptChangesNotifier] will call first applicable [ScriptChangeListener] when editor is activated or document changed.
 * (it treated as applicable if [editorActivated] or [documentChanged] will return true).
 *
 * Listener may call [ScriptConfigurationUpdater] to invalidate configuration and schedule reloading.
 *
 * @see DefaultScriptConfigurationManager for more details.
 */
interface ScriptChangeListener {
    fun editorActivated(file: KtFile, updater: ScriptConfigurationUpdater): Boolean
    fun documentChanged(file: KtFile, updater: ScriptConfigurationUpdater): Boolean
}