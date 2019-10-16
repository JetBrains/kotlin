/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.idea.codeInsight.gradle.MultiplePluginVersionGradleImportingTestCase.Companion.MINIMAL_SUPPORTED_VERSION

//BUNCH 181
fun MultiplatformProjectImportingTest.shouldRunTest(kotlinPluginVersion: String, gradleVersion: String): Boolean {
    return MINIMAL_SUPPORTED_VERSION == kotlinPluginVersion
}

fun NewMultiplatformProjectImportingTest.shouldRunTest(kotlinPluginVersion: String, gradleVersion: String): Boolean {
    return !gradleVersion.startsWith("3.")
}

fun KaptImportingTest.isAndroidStudio() = false