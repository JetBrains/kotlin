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

class BtaClassesClashTest {
    @BtaVersionsOnlyCompilationTest
    fun testNumberOfAvailableApiClasses() {
        val fqn = KotlinLogger::class.java.name // FQN of an API class added quite a long time ago
        val resourceName = fqn.replace('.', '/') + ".class"

        val urls = btaClassloader
            .getResources(resourceName)
            .toList()

        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val expectedNumber = if (KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) >= KotlinToolingVersion(2, 4, 0, "snapshot")) {
            1
        } else {
            2
        }
        assertEquals(expectedNumber, urls.size) {
            "Expected exactly one $fqn on classpath, but found ${urls.size}: $urls"
        }
    }
}