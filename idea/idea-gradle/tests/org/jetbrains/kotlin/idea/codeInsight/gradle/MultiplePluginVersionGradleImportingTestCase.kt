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
import java.util.*

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

    @JvmField
    @Parameterized.Parameter(1)
    var gradleKotlinPluginVersionType: String = MINIMAL_SUPPORTED_VERSION

    val gradleKotlinPluginVersion: String
        get() = KOTLIN_GRADLE_PLUGIN_VERSION_DESCRIPTION_TO_VERSION[gradleKotlinPluginVersionType] ?: gradleKotlinPluginVersionType

    companion object {
        const val MINIMAL_SUPPORTED_VERSION = "minimal"// minimal supported version
        const val LATEST_STABLE_VERSUON = "latest stable"
        const val LATEST_SUPPORTED_VERSION = "master"// gradle plugin from current build
        private val KOTLIN_GRADLE_PLUGIN_VERSIONS = listOf(MINIMAL_SUPPORTED_VERSION, LATEST_STABLE_VERSUON/*, LATEST_SUPPORTED_VERSION*/)
        private val KOTLIN_GRADLE_PLUGIN_VERSION_DESCRIPTION_TO_VERSION = mapOf(
            MINIMAL_SUPPORTED_VERSION to MINIMAL_SUPPORTED_GRADLE_PLUGIN_VERSION,
            LATEST_STABLE_VERSUON to LATEST_STABLE_GRADLE_PLUGIN_VERSION,
            LATEST_SUPPORTED_VERSION to "master"
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: Gradle-{0}, KotlinGradlePlugin-{1}")
        fun data(): Collection<Array<Any>> {
            return AbstractModelBuilderTest.SUPPORTED_GRADLE_VERSIONS.flatMap { gradleVersion ->
                KOTLIN_GRADLE_PLUGIN_VERSIONS.map { kotlinVersion ->
                    arrayOf<Any>(
                        gradleVersion[0],
                        kotlinVersion
                    )
                }
            }.toList()
        }
    }

    override fun configureByFiles(properties: Map<String, String>?): List<VirtualFile> {
        val unitedProperties = HashMap(properties ?: emptyMap())
        unitedProperties["kotlin_plugin_version"] = gradleKotlinPluginVersion
        unitedProperties["kotlin_plugin_repositories"] = """
            google()
            jcenter()
            maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }
            maven { url 'http://dl.bintray.com/kotlin/kotlin-eap' }
        	mavenLocal()
            //maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
        """.trimIndent()
        return super.configureByFiles(unitedProperties)
    }
}

