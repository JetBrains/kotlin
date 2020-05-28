/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.roots

import org.junit.Test

class GradleBuildRootsLocatorTest : AbstractGradleBuildRootsLocatorTest() {
    @Test
    fun testUnlinkedBuildGradleKtsNearProjectRoot() {
        newImportedGradleProject("imported", relativeScripts = listOf())
        assert(findScriptBuildRoot("imported/build.gradle.kts")?.root is GradleBuildRoot.Unlinked)
    }
}