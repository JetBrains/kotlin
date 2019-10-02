/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.listener

import org.jetbrains.kotlin.psi.KtFile

interface ScriptConfigurationUpdater {
    /**
     *  Ensure that configuration for [file] is up-to-date,
     *  or new configuration loading is started
     */
    fun ensureUpToDatedConfigurationSuggested(file: KtFile)

    /**
     * Check that configurations for given [files] is up-to-date or try synchronously update
     * them. Synchronous update will be occurred only if auto apply is enabled and
     * configuration for given file will not be loaded in background.
     *
     * @return true, if all files are already up-to-date or updated during this call
     */
    fun ensureConfigurationUpToDate(files: List<KtFile>): Boolean

    /**
     * Invalidate current configuration and start loading new
     */
    fun forceConfigurationReload(file: KtFile)
}