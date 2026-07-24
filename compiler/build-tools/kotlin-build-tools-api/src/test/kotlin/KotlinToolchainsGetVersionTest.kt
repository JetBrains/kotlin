/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

@DisplayName("KotlinToolchains.getVersion() tests")
class KotlinToolchainsGetVersionTest {

    @Test
    @DisplayName("Should return a version string matching the semver format")
    fun returnsSemverFormattedVersion() {
        val version = KotlinToolchains.getVersion()
        val semverPattern = Regex("""\d+\.\d+\.\d+""")
        assertTrue(semverPattern.matches(version), "getVersion() should match SemVer format (e.g. '2.4.20'), but was: '$version'")
    }

}
