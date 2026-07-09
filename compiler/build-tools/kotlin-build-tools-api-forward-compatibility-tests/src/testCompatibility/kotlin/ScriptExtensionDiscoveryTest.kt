/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.operations.DiscoverScriptExtensionsOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertThrows

class ScriptExtensionDiscoveryTest : BaseCompilationTest() {

    @DisplayName("Test that script extension discovery operation is available from Kotlin compiler version 2.4.0")
    @BtaVersionsOnlyCompilationTest
    fun testScriptExtensionDiscoveryAvailability(kotlinToolchain: KotlinToolchains) {
        val version = KotlinToolingVersion(kotlinToolchain.getCompilerVersion())
        if (version < KotlinToolingVersion(2, 4, 0, "snapshot")
            && kotlinToolchain::class.simpleName != "KotlinToolchainsV1Adapter") {
            assertThrows<UnsupportedOperationException> {
                kotlinToolchain.jvm.discoverScriptExtensionsOperationBuilder(emptyList())
            }
        } else {
            assertInstanceOf<DiscoverScriptExtensionsOperation>(kotlinToolchain.jvm.discoverScriptExtensionsOperationBuilder(emptyList()))
        }

    }
}
