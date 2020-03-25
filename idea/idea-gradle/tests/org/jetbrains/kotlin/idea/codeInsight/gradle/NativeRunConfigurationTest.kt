/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.gradle.GradleDaemonAnalyzerTestCase
import org.jetbrains.kotlin.test.TagsTestDataUtil
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.junit.Test
import org.junit.runners.Parameterized
import java.net.URL
import java.util.*

class NativeRunConfigurationTest : GradleImportingTestCase() {
    private companion object {
        private const val DEFAULT_PLUGIN_VERSION = "1.4-M2"
        private val latestPluginVersion by lazy {
            PluginVersionDownloader.getLatestDevVersion() ?: DEFAULT_PLUGIN_VERSION
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: Gradle-{0}, KotlinGradlePlugin-{1}")
        fun data(): Collection<Array<Any>> = listOf(arrayOf("6.0.1", latestPluginVersion))
    }

    override fun testDataDirName(): String = "nativeRunConfiguration"

    @JvmField
    @Parameterized.Parameter(1)
    var kotlinGradlePluginVersion: String = DEFAULT_PLUGIN_VERSION

    @Test
    fun multiplatformNativeRunGutter() {
        doTest()
    }

    @Test
    fun customEntryPointWithoutRunGutter() {
        doTest()
    }

    override fun configureByFiles(properties: Map<String, String>?): List<VirtualFile> {
        val unitedProperties = HashMap(properties ?: emptyMap())
        unitedProperties["kotlin_plugin_version"] = kotlinGradlePluginVersion
        return super.configureByFiles(unitedProperties)
    }

    private fun doTest() {
        val files = importProjectFromTestData()
        val project = myTestFixture.project

        org.jetbrains.kotlin.gradle.checkFiles(
            files.filter { it.extension == "kt" },
            project,
            object : GradleDaemonAnalyzerTestCase(
                testLineMarkers = true,
                checkWarnings = false,
                checkInfos = false,
                rootDisposable = testRootDisposable
            ) {
                override fun renderAdditionalAttributeForTag(tag: TagsTestDataUtil.TagInfo<*>): String? {
                    val lineMarkerInfo = tag.data as? LineMarkerInfo<*> ?: return null
                    val gradleRunConfigs = lineMarkerInfo.extractConfigurations().filter { it.configuration is GradleRunConfiguration }
                    val runConfig = gradleRunConfigs.singleOrNull() // can we have more than one?

                    val settings = (runConfig?.configurationSettings?.configuration as? GradleRunConfiguration)?.settings ?: return null

                    return "settings=\"${settings}\""
                }

            }
        )
    }

    private fun LineMarkerInfo<*>.extractConfigurations(): List<ConfigurationFromContext> {
        val location = PsiLocation(element)
        val emptyContext = ConfigurationContext.createEmptyContextForLocation(location)
        return emptyContext.configurationsFromContext.orEmpty()
    }
}

object PluginVersionDownloader {
    fun getLatestEapVersion() = downloadVersions(EAP_URL).lastOrNull()
    fun getLatestDevVersion() = downloadVersions(DEV_URL).lastOrNull()

    private fun downloadVersions(url: String): List<String> {
        return versionRegexp.findAll(URL(url).readText())
            .map { it.groupValues[1].removeSuffix("/") }
            .filter { it.isNotEmpty() && it[0].isDigit() }
            .toList()
    }

    private const val EAP_URL = "https://dl.bintray.com/kotlin/kotlin-eap/org/jetbrains/kotlin/jvm/org.jetbrains.kotlin.jvm.gradle.plugin/"
    private const val DEV_URL = "https://dl.bintray.com/kotlin/kotlin-dev/org/jetbrains/kotlin/jvm/org.jetbrains.kotlin.jvm.gradle.plugin/"
    private val versionRegexp = """href="([^"\\]+)"""".toRegex()
}