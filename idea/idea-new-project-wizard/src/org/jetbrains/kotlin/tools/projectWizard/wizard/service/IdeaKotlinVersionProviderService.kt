/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.service

import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.framework.ui.ConfigureDialogWithModulesAndVersion
import org.jetbrains.kotlin.tools.projectWizard.core.TaskResult
import org.jetbrains.kotlin.tools.projectWizard.core.asNullable
import org.jetbrains.kotlin.tools.projectWizard.core.compute
import org.jetbrains.kotlin.tools.projectWizard.core.safe
import org.jetbrains.kotlin.tools.projectWizard.core.service.KotlinVersionProviderService
import org.jetbrains.kotlin.tools.projectWizard.core.service.KotlinVersionProviderServiceImpl
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.stream.Collectors

class IdeaKotlinVersionProviderService : KotlinVersionProviderService, IdeaWizardService {
    override fun getKotlinVersion(): Version {
        if (KotlinPluginUtil.isSnapshotVersion()) {
            return VersionsDownloader.getLatestEapOrStableKotlinVersion() ?: KotlinVersionProviderServiceImpl.DEFAULT
        }
        return KotlinPluginUtil.getPluginVersion().let { Version.fromString(it) }
    }
}

private object VersionsDownloader {
    fun getLatestEapOrStableKotlinVersion(): Version? {
        val latestEap = EapVersionDownloader.getLatestEapVersion()
        val latestStable = getLatestStableVersion()
        if (latestEap == null) return latestStable
        if (latestStable == null) return latestEap
        return if (latestEap > latestStable) latestEap else latestStable
    }

    private fun getLatestStableVersion() = safe {
        ConfigureDialogWithModulesAndVersion.loadVersions("1.0.0")
    }.asNullable?.firstOrNull()?.let { Version.fromString(it) }
}

private object EapVersionDownloader {
    fun getLatestEapVersion() = downloadVersions(EAP_URL).firstOrNull()

    private fun downloadPage(url: String): TaskResult<String> = safe {
        BufferedReader(InputStreamReader(URL(url).openStream())).lines().collect(Collectors.joining("\n"))
    }

    @Suppress("SameParameterValue")
    private fun downloadVersions(url: String): List<Version> = compute {
        val (text) = downloadPage(url)
        versionRegexp.findAll(text)
            .map { it.groupValues[1].removeSuffix("/") }
            .filter { it.isNotEmpty() && it[0].isDigit() }
            .map { Version.fromString(it) }
            .toList()
            .asReversed()
    }.asNullable.orEmpty()

    private val EAP_URL = "https://dl.bintray.com/kotlin/kotlin-eap/org/jetbrains/kotlin/jvm/org.jetbrains.kotlin.jvm.gradle.plugin/"
    private val versionRegexp = """href="([^"\\]+)"""".toRegex()
}