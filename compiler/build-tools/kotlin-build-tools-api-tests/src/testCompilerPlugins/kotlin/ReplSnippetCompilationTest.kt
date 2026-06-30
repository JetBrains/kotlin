/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.compileReplSnippetOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.CompileReplSnippetOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.ReplSnippetCompilationResult
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.TestKotlinLogger
import org.jetbrains.kotlin.buildtools.tests.compilation.util.currentKotlinStdlibLocation
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path

/**
 * End-to-end smoke test for the **stateless K2 REPL** transport seam,
 * [CompileReplSnippetOperation] (migration step 3 / Q5d).
 *
 * Unlike `K2ReplStatelessCompilerTest` (in `:kotlin-scripting-compiler`, which exercises the
 * prototype compiler in-process within that module), this test drives the op through the
 * **real BTA boundary**: the operation is created from the
 * `JvmPlatformToolchain` and executed by a `BuildSession` loaded from the shaded
 * `kotlin-build-tools-impl` jar (`KotlinToolchains.loadImplementation`, where scripting-compiler
 * classes are relocated to `org.jetbrains.kotlin.buildtools.internal.scripting.*`). It is therefore
 * the proof that the artifact wire-codec + op transport actually round-trips across the process-style
 * seam an out-of-process IDE/build-system consumer would use — not merely within one classloader.
 *
 * Both execution policies are exercised: in-process (drives `K2ReplStatelessCompiler` directly) and
 * with-daemon (the snippet is compiled on the regular `CompileService.compile(...)` path, switched
 * into snippet mode by scripting-plugin options, with the artifact exchanged through files —
 * migration step 3 / Q5d). The daemon variant is the proof that a snippet sequence compiles
 * out-of-process without any REPL-specific daemon RMI.
 *
 * The deferral of this test (and the reason it lives here rather than in `:kotlin-scripting-compiler`'s
 * test source set) is recorded in
 * `plugins/scripting/.ai/iterations/2026-05-28c_stateless-repl-bta-transport.md` §"Follow-ups" #1.
 */
class ReplSnippetCompilationTest : BaseCompilationTest() {

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Stateless REPL: compile a multi-snippet sequence end-to-end through the BTA transport")
    fun smokeTestStatelessReplSnippetCompilation(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val executionPolicy = strategyConfig.second
        val stdlib = currentKotlinStdlibLocation

        // snippet 1: `val x = 42` — no prior snippets.
        val artifact1 = compileSnippetArtifact(toolchain, executionPolicy, emptyList(), "val x = 42", "s1.repl.kts", stdlib)
        assertTrue(artifact1.isNotEmpty(), "snippet 1 artifact bytes must be non-empty")

        // snippet 2: `val y = x + 1` — references a declaration from snippet 1 across the transport
        // boundary. A `Success` with non-empty bytes proves the prior artifact was decoded and its
        // `x` was resolvable during snippet 2's compile.
        val artifact2 = compileSnippetArtifact(toolchain, executionPolicy, listOf(artifact1), "val y = x + 1", "s2.repl.kts", stdlib)
        assertTrue(artifact2.isNotEmpty(), "snippet 2 artifact bytes must be non-empty")
        assertFalse(artifact1.contentEquals(artifact2), "distinct snippets must produce distinct artifacts")

        // snippet 3: `x + y` — references declarations from *both* priors. Still a pure compile
        // (no execution), so success here proves the whole prior chain reconstructs over the wire.
        val artifact3 = compileSnippetArtifact(
            toolchain, executionPolicy, listOf(artifact1, artifact2), "x + y", "s3.repl.kts", stdlib
        )
        assertTrue(artifact3.isNotEmpty(), "snippet 3 artifact bytes must be non-empty")
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Stateless REPL: a snippet that references an undefined symbol returns a structured Failure")
    fun replSnippetCompilationSurfacesErrors(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val toolchain = strategyConfig.first
        val executionPolicy = strategyConfig.second
        val stdlib = currentKotlinStdlibLocation

        // `noSuchSymbol` is unresolved and produces no usable class files, so the stateless compiler
        // returns a hard failure. The op surfaces this as a structured `Failure` carrying the
        // diagnostics — it does NOT throw for a plain compile failure.
        val result = compileSnippet(toolchain, executionPolicy, emptyList(), "noSuchSymbol + 1", "bad.repl.kts", stdlib)

        val failure = assertInstanceOf(
            ReplSnippetCompilationResult.Failure::class.java, result,
            "an unresolved-symbol snippet must produce a Failure"
        )
        assertTrue(
            failure.diagnostics.any { it.severity == CompilerMessageRenderer.Severity.ERROR },
            "a Failure must carry at least one ERROR diagnostic"
        )
        val message = failure.diagnostics.joinToString("\n") { it.message }
        assertTrue(
            message.contains("noSuchSymbol"),
            "diagnostics must reference the offending symbol; was: $message"
        )
    }

    private fun compileSnippet(
        toolchain: KotlinToolchains,
        executionPolicy: ExecutionPolicy,
        priorSnippets: List<ByteArray>,
        source: String,
        name: String,
        stdlib: Path,
    ): ReplSnippetCompilationResult {
        val operation = toolchain.jvm.compileReplSnippetOperation(priorSnippets, source, name) {
            this[CompileReplSnippetOperation.ADDITIONAL_CLASSPATH] = listOf(stdlib)
        }
        return toolchain.createBuildSession().use { session ->
            session.executeOperation(operation, executionPolicy, TestKotlinLogger())
        }
    }

    /** Compiles a snippet expected to succeed, asserting [ReplSnippetCompilationResult.Success] and returning its artifact bytes. */
    private fun compileSnippetArtifact(
        toolchain: KotlinToolchains,
        executionPolicy: ExecutionPolicy,
        priorSnippets: List<ByteArray>,
        source: String,
        name: String,
        stdlib: Path,
    ): ByteArray {
        val result = compileSnippet(toolchain, executionPolicy, priorSnippets, source, name, stdlib)
        val success = assertInstanceOf(
            ReplSnippetCompilationResult.Success::class.java, result,
            "snippet '$name' was expected to compile; diagnostics: " +
                    result.diagnostics.joinToString("\n") { "${it.severity}: ${it.message}" }
        )
        return success.artifact
    }
}
