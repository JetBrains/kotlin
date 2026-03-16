/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class LookupTrackerTest : BaseCompilationTest() {
    @Test
    @DisplayName("LOOKUP_TRACKER can be set using the deprecated Option")
    fun setLookupTracker() {
        val jvmOperation = toolchain.jvm.createJvmCompilationOperation(emptyList(), Paths.get(""))
        val lookupTracker = object : CompilerLookupTracker {
            override fun clear() {}

            override fun recordLookup(
                filePath: String,
                scopeFqName: String,
                scopeKind: CompilerLookupTracker.ScopeKind,
                name: String,
            ) {
            }
        }
        jvmOperation[JvmCompilationOperation.LOOKUP_TRACKER] = lookupTracker
        assertEquals(lookupTracker, jvmOperation[JvmCompilationOperation.LOOKUP_TRACKER])
    }
}
