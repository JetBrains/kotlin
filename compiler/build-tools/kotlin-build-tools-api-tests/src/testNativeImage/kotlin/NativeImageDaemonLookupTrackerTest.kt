/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

@DisplayName("Lookup tracking in the native image daemon execution policy")
class NativeImageDaemonLookupTrackerTest : BaseCompilationTest() {
    val nativeImageDistPath: Path = ForTestCompileRuntime.kotlinNativeImageDistForTests().toPath()

    @Test
    @DisplayName("Lookup tracker records lookups in non-incremental mode")
    @TestMetadata("basic-multimodule-project/module-1")
    fun lookupsNonIncremental() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        runSingleShotNativeImageDaemonTest(kotlinToolchains, nativeImageDistPath) { daemonPolicy, _ ->
            jvmProject(kotlinToolchains to daemonPolicy) {
                val module = module("basic-multimodule-project/module-1")
                val lookupRecorded = AtomicBoolean(false)
                val lookupTracker = lookupTracker(lookupRecorded)
                module.compile(
                    compilationConfigAction = { builder: BaseCompilationOperation.Builder ->
                        builder[BaseCompilationOperation.LOOKUP_TRACKER] = lookupTracker
                    }
                ) {
                    assertTrue(lookupRecorded.get()) { "Lookup tracker didn't produce any output" }
                }
            }
        }
    }

    @Test
    @DisplayName("Lookup tracker records lookups in incremental mode")
    @TestMetadata("basic-multimodule-project/module-1")
    fun lookupsIncremental() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        runSingleShotNativeImageDaemonTest(kotlinToolchains, nativeImageDistPath) { daemonPolicy, _ ->
            jvmProject(kotlinToolchains to daemonPolicy) {
                val module = module("basic-multimodule-project/module-1")
                val lookupRecorded = AtomicBoolean(false)
                val lookupTracker = lookupTracker(lookupRecorded)
                module.compileIncrementally(
                    SourcesChanges.Unknown,
                    compilationConfigAction = { builder: BaseCompilationOperation.Builder ->
                        builder[BaseCompilationOperation.LOOKUP_TRACKER] = lookupTracker
                    }
                ) {
                    assertTrue(lookupRecorded.get()) { "Lookup tracker didn't produce any output" }
                }
            }
        }
    }

    private fun lookupTracker(lookupRecorded: AtomicBoolean): CompilerLookupTracker = object : CompilerLookupTracker {
        override fun recordLookup(
            filePath: String,
            scopeFqName: String,
            scopeKind: CompilerLookupTracker.ScopeKind,
            name: String,
        ) {
            lookupRecorded.set(true)
        }

        override fun clear() {
        }
    }
}
