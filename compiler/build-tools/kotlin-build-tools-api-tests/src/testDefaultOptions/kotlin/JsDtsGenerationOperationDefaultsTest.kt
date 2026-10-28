/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.defaults

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JsModuleKind
import org.jetbrains.kotlin.buildtools.api.js.JsDtsCompilationStrategy
import org.jetbrains.kotlin.buildtools.api.js.JsDtsGranularity
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain.Companion.js
import org.jetbrains.kotlin.buildtools.api.js.operations.JsDtsGenerationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.buildtools.tests.defaults.BuildOperationDefaultsTest.Companion.DEFAULT_METRICS_COLLECTOR
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInJs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

@MustRunOnChangesInJs
class JsDtsGenerationOperationDefaultsTest {
    @Test
    fun testDefaultOptions() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val operation = kotlinToolchains.js.jsDtsGenerationOperationBuilder(listOf(Path("main.klib")), Path(".")).build()
        assertEquals(DEFAULT_METRICS_COLLECTOR, operation[BuildOperation.METRICS_COLLECTOR])
        assertEquals(JsModuleKind.UMD, operation[JsDtsGenerationOperation.MODULE_KIND])
        assertEquals(JsDtsGranularity.WHOLE_PROGRAM, operation[JsDtsGenerationOperation.GRANULARITY])
        assertEquals(JsDtsCompilationStrategy.MERGED, operation[JsDtsGenerationOperation.TS_COMPILATION_STRATEGY])
        assertEquals(false, operation[JsDtsGenerationOperation.COMPILE_LONG_AS_BIG_INT])
        assertEquals(false, operation[JsDtsGenerationOperation.IMPLEMENT_INTERFACES])
        assertEquals(false, operation[JsDtsGenerationOperation.EXPORT_SUSPEND_LAMBDAS])
        assertEquals(false, operation[JsDtsGenerationOperation.DATA_CLASS_COPY_RESPECTS_CONSTRUCTOR_VISIBILITY])
        // TODO (KT-90366): check EXPORT_UNTYPED_AS_UNKNOWN once reading its default value is fixed
    }
}
