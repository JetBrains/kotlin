/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.project
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName

class LookupTrackerTest : BaseCompilationTest() {

    private fun assumeSupportsLookups(strategyConfig: CompilerExecutionStrategyConfiguration, incremental: Boolean) {
        val currentKotlinVersion = KotlinToolingVersion(strategyConfig.first.getCompilerVersion())
        assumeTrue(
            currentKotlinVersion >=
                    when (strategyConfig.second) {
                        is ExecutionPolicy.InProcess if incremental -> KotlinToolingVersion(2, 3, 0, null)
                        is ExecutionPolicy.InProcess -> KotlinToolingVersion(2, 3, 20, null)
                        is ExecutionPolicy.WithDaemon -> KotlinToolingVersion(2, 3, 20, null)
                    }
        )
    }

    @DisplayName("Lookup tracker produces output in non-incremental mode")
    @BtaV2StrategyAgnosticCompilationTest
    fun lookupsNonIncremental(strategyConfig: CompilerExecutionStrategyConfiguration) {
        assumeSupportsLookups(strategyConfig, incremental = false)
        project(strategyConfig) {
            val module1 = module("jvm-module-1")
            var lookupRecorded = false
            val lookupTracker = object : CompilerLookupTracker {
                override fun recordLookup(
                    filePath: String,
                    scopeFqName: String,
                    scopeKind: CompilerLookupTracker.ScopeKind,
                    name: String,
                ) {
                    lookupRecorded = true
                }

                override fun clear() {
                }

            }
            module1.compile(compilationConfigAction = { builder: JvmCompilationOperation.Builder ->
                builder[JvmCompilationOperation.LOOKUP_TRACKER] = lookupTracker
            }) {
                assertTrue(lookupRecorded) { "Lookup tracker didn't produce any output" }
            }
        }
    }

    @DisplayName("Lookup tracker produces output in incremental mode")
    @BtaV2StrategyAgnosticCompilationTest
    fun lookupsIncremental(strategyConfig: CompilerExecutionStrategyConfiguration) {
        assumeSupportsLookups(strategyConfig, incremental = true)
        project(strategyConfig) {
            val module1 = module("jvm-module-1")
            var lookupRecorded = false
            val lookupTracker = object : CompilerLookupTracker {
                override fun recordLookup(
                    filePath: String,
                    scopeFqName: String,
                    scopeKind: CompilerLookupTracker.ScopeKind,
                    name: String,
                ) {
                    lookupRecorded = true
                }

                override fun clear() {
                }

            }
            module1.compileIncrementally(SourcesChanges.Unknown, compilationConfigAction = { builder: JvmCompilationOperation.Builder ->
                builder[JvmCompilationOperation.LOOKUP_TRACKER] = lookupTracker
            }) {
                assertTrue(lookupRecorded) { "Lookup tracker didn't produce any output" }
            }
        }
    }
}
