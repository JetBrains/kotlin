/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.nativeImageProcessExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.MetadataProject
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Error handling of the native image process execution policy")
class NativeImageProcessErrorHandlingTest : BaseCompilationTest() {
    private val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)

    @Test
    @DisplayName("The native image process strategy only supports JVM compilation")
    @TestMetadata("empty")
    fun testNonJvmTargetIsRejected() {
        val policy = kotlinToolchains.nativeImageProcessExecutionPolicy {
            this[ExecutionPolicy.NativeImageProcess.DIST_PATH] = workingDirectory.resolve("dist")
        }

        metadataProjectWithoutNativeImageProcessAssumption(kotlinToolchains to policy) {
            val module = module("empty")
            module.compile {
                expectFail()
                assertLogContainsPatterns(
                    LogLevel.ERROR,
                    Regex(".*The native image process strategy only supports JVM compilation\\."),
                )
            }
        }
    }

    @Test
    @DisplayName("The native image process strategy does not support incremental compilation")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testIncrementalCompilationIsRejected() {
        val policy = kotlinToolchains.nativeImageProcessExecutionPolicy {
            this[ExecutionPolicy.NativeImageProcess.DIST_PATH] = workingDirectory.resolve("dist")
        }

        jvmProject(kotlinToolchains to policy) {
            val module = module("basic-multimodule-project/module-1")
            module.compileIncrementally(SourcesChanges.Unknown) {
                expectFail()
                assertLogContainsPatterns(
                    LogLevel.ERROR,
                    Regex(".*Incremental compilation is not supported by the native image process strategy\\."),
                )
            }
        }
    }

    @Test
    @DisplayName("The native image process strategy does not support lookup tracking")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testLookupTrackerIsRejected() {
        val policy = kotlinToolchains.nativeImageProcessExecutionPolicy {
            this[ExecutionPolicy.NativeImageProcess.DIST_PATH] = workingDirectory.resolve("dist")
        }

        jvmProject(kotlinToolchains to policy) {
            val module = module("basic-multimodule-project/module-1")
            val lookupTracker = object : CompilerLookupTracker {
                override fun recordLookup(
                    filePath: String,
                    scopeFqName: String,
                    scopeKind: CompilerLookupTracker.ScopeKind,
                    name: String,
                ) {
                }

                override fun clear() {
                }
            }
            module.compile(
                compilationConfigAction = { builder: BaseCompilationOperation.Builder ->
                    builder[BaseCompilationOperation.LOOKUP_TRACKER] = lookupTracker
                }
            ) {
                expectFail()
                assertLogContainsPatterns(
                    LogLevel.ERROR,
                    Regex(".*Lookup tracking is not supported by the native image process strategy\\."),
                )
            }
        }
    }

    @Test
    @DisplayName("A missing native image distribution is reported as a compiler internal error")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testMissingDistIsReported() {
        val policy = kotlinToolchains.nativeImageProcessExecutionPolicy {
            this[ExecutionPolicy.NativeImageProcess.DIST_PATH] = workingDirectory.resolve("nonexistent-dist")
        }

        jvmProject(kotlinToolchains to policy) {
            val module = module("basic-multimodule-project/module-1")
            module.compile {
                expectCompilationResult(CompilationResult.COMPILER_INTERNAL_ERROR)
                assertLogContainsPatterns(
                    LogLevel.ERROR,
                    Regex(".*Failed to run the native image compiler\\."),
                )
            }
        }
    }

    private fun BaseCompilationTest.metadataProjectWithoutNativeImageProcessAssumption(
        executionStrategy: CompilerExecutionStrategyConfiguration,
        action: MetadataProject.() -> Unit,
    ) {
        MetadataProject(executionStrategy.first, executionStrategy.second, workingDirectory).use { project ->
            project.action()
        }
    }
}
