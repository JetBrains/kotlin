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

        //should be extended with LATEST_SUPPORTED_VERSION
        private val KOTLIN_GRADLE_PLUGIN_VERSIONS = listOf(MINIMAL_SUPPORTED_VERSION, LATEST_STABLE_VERSUON)
        private val KOTLIN_GRADLE_PLUGIN_VERSION_DESCRIPTION_TO_VERSION = mapOf(
            MINIMAL_SUPPORTED_VERSION to MINIMAL_SUPPORTED_GRADLE_PLUGIN_VERSION,
            LATEST_STABLE_VERSUON to LATEST_STABLE_GRADLE_PLUGIN_VERSION,
            LATEST_SUPPORTED_VERSION to readPluginVersion()
        )

        private fun readPluginVersion() =
            File("libraries/tools/kotlin-gradle-plugin/build/libs").listFiles()?.map { it.name }?.firstOrNull { it.contains("-original.jar") }?.replace(
                "kotlin-gradle-plugin-",
                ""
            )?.replace("-original.jar", "") ?: "1.4-SNAPSHOT"

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

    fun repositories(useKts: Boolean, useMaster: Boolean): String {
        val flatDirs = arrayOf(
            "libraries/tools/kotlin-gradle-plugin/build/libs",
            "libraries/tools/kotlin-gradle-plugin/build/libs",
            "prepare/compiler-client-embeddable/build/libs",
            "prepare/compiler-embeddable/build/libs",
            "libraries/tools/kotlin-gradle-plugin-api/build/libs",
            "compiler/compiler-runner/build/libs",
            "libraries/tools/kotlin-gradle-plugin-model/build/libs",
            "plugins/scripting/scripting-compiler-embeddable/build/libs",
            "konan/utils/build/libs"
        )
        val customRepositories = arrayOf("https://dl.bintray.com/kotlin/kotlin-dev", "http://dl.bintray.com/kotlin/kotlin-eap")
        val customMavenRepositories = customRepositories.map { if (useKts) "maven { setUrl(\"$it\") }" else "maven { url '$it' } " }.joinToString("\n")
        val baseFolder = File(".").absolutePath.replace("\\", "/")
        val quote = if (useKts) '"' else '\''
        val flatDirRepositories = if (useMaster)
            flatDirs.map { "$quote$baseFolder/$it$quote" }.joinToString(
                ",\n",
                if (useKts) "flatDir { dirs(" else "flatDir { dirs ",
                if (useKts) ")}" else "}"
            )
        else
            ""
        return """
            google()
            jcenter()
            $customMavenRepositories
            $flatDirRepositories
        """.trimIndent()
    }

    override fun configureByFiles(properties: Map<String, String>?): List<VirtualFile> {
        val unitedProperties = HashMap(properties ?: emptyMap())
        unitedProperties["kotlin_plugin_version"] = gradleKotlinPluginVersion

        unitedProperties["kotlin_plugin_repositories"] = repositories(false, gradleKotlinPluginVersionType == LATEST_SUPPORTED_VERSION)
        unitedProperties["kts_kotlin_plugin_repositories"] = repositories(true, gradleKotlinPluginVersionType == LATEST_SUPPORTED_VERSION)

        unitedProperties["extraPluginDependencies"] = if (gradleKotlinPluginVersionType == LATEST_SUPPORTED_VERSION)
            """
                classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$gradleKotlinPluginVersion")
                classpath("org.jetbrains.kotlin:kotlin-compiler-embeddable:$gradleKotlinPluginVersion")
                classpath("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$gradleKotlinPluginVersion")
                classpath("org.jetbrains.kotlin:kotlin-native-utils:$gradleKotlinPluginVersion")
                classpath("org.jetbrains.kotlin:kotlin-compiler-runner:$gradleKotlinPluginVersion")
                classpath("org.jetbrains.kotlin:kotlin-compiler-client-embeddable:$gradleKotlinPluginVersion")
                classpath("org.jetbrains.kotlin:kotlin-gradle-plugin-model:$gradleKotlinPluginVersion")
            """.trimIndent()
        else
            ""

        unitedProperties["kts_resolution_strategy"] = if (gradleKotlinPluginVersionType == LATEST_SUPPORTED_VERSION)
            """resolutionStrategy {
                eachPlugin {
                    if(requested.id.id == "org.jetbrains.kotlin.multiplatform") {
                        useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:$gradleKotlinPluginVersion")
                    }
                }
            }""".trimIndent()
        else ""
        return super.configureByFiles(unitedProperties)
    }
}

