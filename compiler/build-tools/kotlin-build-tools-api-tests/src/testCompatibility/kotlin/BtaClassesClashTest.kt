/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName

class BtaClassesClashTest {
    @DisplayName("Number of BTA API class copies on classpath is correct for compiler version")
    @BtaVersionsOnlyCompilationTest
    fun testNumberOfAvailableApiClasses(toolchain: KotlinToolchains) {
        val fqn = KotlinLogger::class.java.name // FQN of an API class added quite a long time ago
        val resourceName = fqn.replace('.', '/') + ".class"

        val urls = btaClassloader
            .getResources(resourceName)
            .toList()

        val expectedNumber = if (KotlinToolingVersion(toolchain.getCompilerVersion()) >= KotlinToolingVersion(2, 4, 0, "snapshot")) {
            1
        } else {
            2
        }
        assertEquals(expectedNumber, urls.size) {
            "Expected $expectedNumber instance(s) of $fqn on classpath, but found ${urls.size}: $urls"
        }
    }
}