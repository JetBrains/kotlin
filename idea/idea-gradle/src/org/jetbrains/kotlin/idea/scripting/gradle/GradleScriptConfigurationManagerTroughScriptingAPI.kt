/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.LoadScriptConfigurationNotificationFactory
import org.jetbrains.kotlin.idea.core.script.configuration.loader.DefaultScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.utils.BackgroundExecutor
import org.jetbrains.kotlin.idea.core.script.configuration.utils.DefaultBackgroundExecutor
import org.jetbrains.kotlin.idea.core.script.configuration.utils.TestingBackgroundExecutor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition

/**
 * Standard implementation of gradle scripts with For Gradle Version less than 6.0
 *
 * ### Loaind initiation
 *
 * We show the notification in editor.
 * Only after user click the loading will be started.
 * This behavior may be disabled by enabling "auto reload" in project settings.
 * When enabled, all scripts will be immediately added to queue, without any notification.
 *
 * Notification will be displayed when configuration is going to be out dated. First configuration will be loaded
 * without notification.
 *
 * ## Loading
 *
 * When requested, configuration will be loaded using Scripting API
 *
 * Each script will be added to queue and than executed asynchronous in background thread (by [BackgroundExecutor]).
 *
 * ## Applying
 *
 * Loaded configuration will be applied immediately.
 *
 */
internal class GradleScriptConfigurationManagerTroughScriptingAPI(project: Project) : GradleScriptConfigurationManager(project) {

    private val defaultLoader = DefaultScriptConfigurationLoader(project)

    private val backgroundExecutor: BackgroundExecutor =
        if (ApplicationManager.getApplication().isUnitTestMode) TestingBackgroundExecutor(rootsIndexer)
        else DefaultBackgroundExecutor(project, rootsIndexer)

    override fun reloadConfiguration(
        file: KtFile,
        vFile: VirtualFile,
        definition: ScriptDefinition,
        forceSync: Boolean,
        postponeLoading: Boolean
    ) {
        if (postponeLoading) {
            LoadScriptConfigurationNotificationFactory.showNotification(vFile, project) {
                addToQueue(file, vFile, definition)
            }
        } else {
            addToQueue(file, vFile, definition)
        }
    }

    private fun addToQueue(
        file: KtFile,
        virtualFile: VirtualFile,
        scriptDefinition: ScriptDefinition
    ) {
        backgroundExecutor.ensureScheduled(virtualFile) {
            val cached = getCachedConfigurationState(virtualFile)

            val applied = cached?.applied
            if (applied != null && applied.inputs.isUpToDate(project, virtualFile)) {
                // in case user reverted to applied configuration
                saveConfiguration(virtualFile, applied)
            } else if (cached == null || !cached.isUpToDate(project, virtualFile)) {
                loadDependencies(file, scriptDefinition)
            }
        }
    }

    override fun loadDependencies(
        ktFile: KtFile,
        scriptDefinition: ScriptDefinition
    ) {
        val vFile = ktFile.originalFile.virtualFile

        val result = defaultLoader.getConfigurationThroughScriptingApi(ktFile, vFile, scriptDefinition)

        LoadScriptConfigurationNotificationFactory.hideNotification(vFile, project)

        saveConfiguration(vFile, result)
    }
}