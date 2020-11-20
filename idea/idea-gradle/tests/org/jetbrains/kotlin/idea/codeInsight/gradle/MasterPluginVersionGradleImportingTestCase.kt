/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * This TestCase implements possibility to test import with different versions of gradle and master
 * version of gradle kotlin plugin
 */
package org.jetbrains.kotlin.idea.codeInsight.gradle

import org.junit.runners.Parameterized

abstract class MasterPluginVersionGradleImportingTestCase : MultiplePluginVersionGradleImportingTestCase() {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: Gradle-{0}, KotlinGradlePlugin-{1}")
        fun data(): Collection<Array<Any>> {
            return (AbstractModelBuilderTest.SUPPORTED_GRADLE_VERSIONS).map { gradleVersion ->
                arrayOf<Any>(
                    gradleVersion[0],
                    LATEST_SUPPORTED_VERSION
                )
            }.toList()
        }
    }
}

