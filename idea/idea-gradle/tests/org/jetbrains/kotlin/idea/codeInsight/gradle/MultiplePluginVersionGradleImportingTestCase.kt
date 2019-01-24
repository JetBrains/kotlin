/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

/**
 * This TestCase implements possibility to test import with different versions of gradle and different
 * versions of gradle kotlin plugin
 */
package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.vfs.VirtualFile
import org.junit.runners.Parameterized
import java.util.*

abstract class MultiplePluginVersionGradleImportingTestCase : GradleImportingTestCase() {

    @JvmField
    @Parameterized.Parameter(1)
    var gradleKotlinPluginVersion: String = MINIMAL_SUPPORTED_VERSION

    companion object {
        const val MINIMAL_SUPPORTED_VERSION = "minimal"// minimal supported version
        private val KOTLIN_GRADLE_PLUGIN_VERSIONS = listOf(MINIMAL_SUPPORTED_VERSION, "1.3.20")

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: Gradle-{0}, KotlinGradlePlugin-{1}")
        fun data(): Collection<Array<Any>> {
            return AbstractModelBuilderTest.SUPPORTED_GRADLE_VERSIONS.flatMap { gradleVersion ->
                if ((gradleVersion[0] as String).startsWith("3.")) {
                    listOf(arrayOf<Any>(gradleVersion[0], MINIMAL_SUPPORTED_VERSION))
                } else {
                    KOTLIN_GRADLE_PLUGIN_VERSIONS.map { kotlinVersion ->
                        arrayOf<Any>(
                            gradleVersion[0],
                            kotlinVersion
                        )
                    }
                }

            }.toList()
        }
    }

    override fun configureByFiles(): List<VirtualFile> {
        val result = ArrayList(super.configureByFiles())
        result.add(
            createProjectSubFile(
                "include.gradle", """
String gradleKotlinPluginVersion(String defaultVersion) {
    return ${if (gradleKotlinPluginVersion.isEmpty() || MINIMAL_SUPPORTED_VERSION == gradleKotlinPluginVersion) "defaultVersion;" else "\"$gradleKotlinPluginVersion\""}
}
ext {
    gradleKotlinPluginVersion = this.&gradleKotlinPluginVersion
}
"""
            )
        )
        return result
    }
}

