/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.*
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths

class LookupTrackerTest : BaseCompilationTest() {

    private fun Project.assumeSupportsLookups(
        incremental: Boolean,
    ) {
        assumeFalse(this is MetadataProject)
        val currentKotlinVersion = KotlinToolingVersion(kotlinToolchain.getCompilerVersion())
        if (this !is JvmProject) {
            assumeTrue(
                currentKotlinVersion >= if (incremental) KotlinToolingVersion(2, 5, 0, "snapshot")
                else KotlinToolingVersion(2, 4, 20, "snapshot")
            )
        } else {
            assumeTrue(
                currentKotlinVersion >= when (defaultStrategyConfig) {
                    is ExecutionPolicy.InProcess if incremental -> KotlinToolingVersion(2, 3, 0, null)
                    is ExecutionPolicy.InProcess -> KotlinToolingVersion(2, 3, 20, null)
                    is ExecutionPolicy.WithDaemon -> KotlinToolingVersion(2, 3, 20, null)
                }
            )
        }
    }

    @DisplayName("Lookup tracker produces output in non-incremental mode")
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    fun lookupsNonIncremental(project: ProjectCreator) {
        project {
            assumeSupportsLookups(incremental = false)
            val module1 = module("basic-multimodule-project/module-1")
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
            module1.compile(compilationConfigAction = { builder: BaseCompilationOperation.Builder ->
                builder[BaseCompilationOperation.LOOKUP_TRACKER] = lookupTracker
            }) {
                assertTrue(lookupRecorded) { "Lookup tracker didn't produce any output" }
            }
        }
    }

    @DisplayName("Lookup tracker produces output in incremental mode")
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    fun lookupsIncremental(project: ProjectCreator) {
        project {
            assumeSupportsLookups(incremental = true)
            val module1 = module("basic-multimodule-project/module-1")
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
            module1.compileIncrementally(SourcesChanges.Unknown, compilationConfigAction = { builder: BaseCompilationOperation.Builder ->
                builder[BaseCompilationOperation.LOOKUP_TRACKER] = lookupTracker
            }) {
                assertTrue(lookupRecorded) { "Lookup tracker didn't produce any output" }
            }
        }
    }

    @DisplayName("LOOKUP_TRACKER can be set using the deprecated Option")
    @BtaVersionsOnlyCompilationTest
    @Suppress("DEPRECATION")
    fun setLookupTrackerDeprecated(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(""))
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
        assertEquals(lookupTracker, jvmOperation.build()[JvmCompilationOperation.LOOKUP_TRACKER])
    }

    @DisplayName("LOOKUP_TRACKER can be set using the newer Option")
    @BtaVersionsOnlyCompilationTest
    fun setLookupTracker(toolchain: KotlinToolchains) {
        val jvmOperation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(""))
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

        jvmOperation[BaseCompilationOperation.LOOKUP_TRACKER] = lookupTracker
        assertEquals(lookupTracker, jvmOperation[BaseCompilationOperation.LOOKUP_TRACKER])
        assertEquals(lookupTracker, jvmOperation.build()[BaseCompilationOperation.LOOKUP_TRACKER])
    }
}
