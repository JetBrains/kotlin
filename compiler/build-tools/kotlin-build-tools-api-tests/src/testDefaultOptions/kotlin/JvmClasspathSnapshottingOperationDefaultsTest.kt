/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.defaults

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.buildtools.tests.defaults.BuildOperationDefaultsTest.Companion.DEFAULT_METRICS_COLLECTOR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class JvmClasspathSnapshottingOperationDefaultsTest {
    @Test
    fun testDefaultOptions() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val operation = kotlinToolchains.jvm.classpathSnapshottingOperationBuilder(Path(".")).build()
        assertEquals(DEFAULT_METRICS_COLLECTOR, operation[BuildOperation.METRICS_COLLECTOR])
        assertEquals(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, operation[JvmClasspathSnapshottingOperation.GRANULARITY])
        assertEquals(true, operation[JvmClasspathSnapshottingOperation.PARSE_INLINED_LOCAL_CLASSES])
    }
}