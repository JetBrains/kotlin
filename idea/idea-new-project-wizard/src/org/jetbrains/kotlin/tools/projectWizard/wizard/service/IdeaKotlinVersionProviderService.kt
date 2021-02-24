/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.idea.framework.ui.ConfigureDialogWithModulesAndVersion
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.core.asNullable
import org.jetbrains.kotlin.tools.projectWizard.core.safe
import org.jetbrains.kotlin.tools.projectWizard.core.service.EapVersionDownloader
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardKotlinVersion
import org.jetbrains.kotlin.tools.projectWizard.core.service.KotlinVersionProviderService
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.wizard.KotlinNewProjectWizardUIBundle
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.runWithProgressBar

class IdeaKotlinVersionProviderService : KotlinVersionProviderService(), IdeaWizardService {
    override fun getKotlinVersion(projectKind: ProjectKind): WizardKotlinVersion {
        if (projectKind == ProjectKind.COMPOSE) {
            return kotlinVersionWithDefaultValues(Versions.KOTLIN_VERSION_FOR_COMPOSE)
        }
        val version = getPatchedKotlinVersion()
            ?: getKotlinVersionFromCompiler()
            ?: VersionsDownloader.downloadLatestEapOrStableKotlinVersion()
            ?: Versions.KOTLIN
        return kotlinVersionWithDefaultValues(version)
    }

    private fun getPatchedKotlinVersion() =
        if (ApplicationManager.getApplication().isInternal) {
            System.getProperty(KOTLIN_COMPILER_VERSION_TAG)?.let { Version.fromString(it) }
        } else {
            null
        }

    private fun getKotlinVersionFromCompiler() =
        KotlinCompilerVersion.getVersion()
            ?.takeUnless { it.contains(SNAPSHOT_TAG, ignoreCase = true) }
            ?.let { Version.fromString(it) }

    companion object {
        private const val KOTLIN_COMPILER_VERSION_TAG = "kotlin.compiler.version"
    }
}

@NonNls
private const val SNAPSHOT_TAG = "snapshot"

private object VersionsDownloader {
    fun downloadLatestEapOrStableKotlinVersion(): Version? =
        runWithProgressBar(KotlinNewProjectWizardUIBundle.message("version.provider.downloading.kotlin.version")) {
            val latestEap = EapVersionDownloader.getLatestEapVersion()
            val latestStable = getLatestStableVersion()
            when {
                latestEap == null -> latestStable
                latestStable == null -> latestEap
                VersionComparatorUtil.compare(latestEap.text, latestStable.text) > 0 -> latestEap
                else -> latestStable
            }
        }

    private fun getLatestStableVersion() = safe {
        ConfigureDialogWithModulesAndVersion.loadVersions("1.0.0")
    }.asNullable?.firstOrNull { !it.contains(SNAPSHOT_TAG, ignoreCase = true) }?.let { Version.fromString(it) }
}
