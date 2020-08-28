/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.roots

import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsLocator.NotificationKind.dontCare
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsLocator.NotificationKind.wasNotImportedAfterCreation
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GradleBuildRootsLocatorTest : AbstractGradleBuildRootsLocatorTest() {
    @Test
    fun testNewBuildGradleKtsNearProjectRoot() {
        // the build.gradle.kts under the project root will be definitive import at next import
        // so, we should not treat it as unlinked
        newImportedGradleProject("imported", relativeScripts = listOf())

        assertNotificationKind("imported/build.gradle.kts", wasNotImportedAfterCreation)
    }

    @Test
    fun testBuildGradleKtsNearProjectRoot() {
        newImportedGradleProject("imported", relativeScripts = listOf("build.gradle.kts"))

        assertNotificationKind("imported/build.gradle.kts", dontCare)
    }
}