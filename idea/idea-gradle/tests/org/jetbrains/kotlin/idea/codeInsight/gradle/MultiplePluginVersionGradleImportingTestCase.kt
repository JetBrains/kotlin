/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * This TestCase implements possibility to test import with different versions of gradle and different
 * versions of gradle kotlin plugin
 */
package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.junit.Rule
import org.junit.runners.Parameterized
import java.io.File
import java.util.*

const val mppImportTestMinVersionForMaster = "6.1+"
const val legacyMppImportTestMinVersionForMaster = "6.1+"

abstract class MultiplePluginVersionGradleImportingTestCase : GradleImportingTestCase() {
    @Rule
    @JvmField
    var pluginVersionMatchingRule = PluginTargetVersionsRule()

    val project: Project
        get() = myProject


    override fun isApplicableTest(): Boolean {
        return pluginVersionMatchingRule.matches(
            gradleVersion, gradleKotlinPluginVersion,
            gradleKotlinPluginVersionType == LATEST_SUPPORTED_VERSION
        )
    }

    override fun importProject() {
        super.importProject(skipIndexing = true)
    }

    @JvmField
    @Parameterized.Parameter(1)
    var gradleKotlinPluginVersionType: String = MINIMAL_SUPPORTED_VERSION

    val gradleKotlinPluginVersion: String
        get() = KOTLIN_GRADLE_PLUGIN_VERSION_DESCRIPTION_TO_VERSION[gradleKotlinPluginVersionType] ?: gradleKotlinPluginVersionType

    companion object {
        const val MINIMAL_SUPPORTED_VERSION = "minimal"// minimal supported version
        const val LATEST_STABLE_VERSUON = "latest stable"
        const val LATEST_SUPPORTED_VERSION = "master"// gradle plugin from current build

        //should be extended with LATEST_SUPPORTED_VERSION
        private val KOTLIN_GRADLE_PLUGIN_VERSIONS = listOf(MINIMAL_SUPPORTED_VERSION, LATEST_STABLE_VERSUON, LATEST_SUPPORTED_VERSION)
        private val KOTLIN_GRADLE_PLUGIN_VERSION_DESCRIPTION_TO_VERSION = mapOf(
            MINIMAL_SUPPORTED_VERSION to MINIMAL_SUPPORTED_GRADLE_PLUGIN_VERSION,
            LATEST_STABLE_VERSUON to LATEST_STABLE_GRADLE_PLUGIN_VERSION,
            LATEST_SUPPORTED_VERSION to readPluginVersion()
        )

        private fun readPluginVersion() =
            File("libraries/tools/kotlin-gradle-plugin/build/libs").listFiles()?.map { it.name }
                ?.firstOrNull { it.contains("-original.jar") }?.replace(
                    "kotlin-gradle-plugin-",
                    ""
                )?.replace("-original.jar", "") ?: "1.5.255-SNAPSHOT"

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: Gradle-{0}, KotlinGradlePlugin-{1}")
        fun data(): Collection<Array<Any>> {
            return (AbstractModelBuilderTest.SUPPORTED_GRADLE_VERSIONS).flatMap { gradleVersion ->
                KOTLIN_GRADLE_PLUGIN_VERSIONS.map { kotlinVersion ->
                    arrayOf<Any>(
                        gradleVersion[0],
                        kotlinVersion
                    )
                }
            }.toList()
        }
    }

    fun repositories(useKts: Boolean): String {
        val customRepositories = arrayOf(
            "https://dl.bintray.com/kotlin/kotlin-dev",
        )
        val customMavenRepositories = customRepositories.map { if (useKts) "maven(\"$it\")" else "maven { url '$it' } " }.joinToString("\n")
        return """
            mavenCentral()
            mavenLocal()
            google()
            jcenter()
            $customMavenRepositories
        """.trimIndent()
    }

    override fun configureByFiles(properties: Map<String, String>?): List<VirtualFile> {
        val unitedProperties = HashMap(properties ?: emptyMap())
        unitedProperties["kotlin_plugin_version"] = gradleKotlinPluginVersion

        unitedProperties["kotlin_plugin_repositories"] = repositories(false)
        unitedProperties["kts_kotlin_plugin_repositories"] = repositories(true)
        return super.configureByFiles(unitedProperties)
    }
}

