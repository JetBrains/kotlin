/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.StubBuilder
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.impl.source.tree.LazyParseableElement
import com.intellij.psi.stubs.StubElementRegistryService
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.streams.asSequence

/**
 * A manual benchmark and large-corpus equivalence check for the light stub builder, driven by environment variables:
 *
 * - `KOTLIN_STUB_BENCHMARK_ROOT` — the root directory to scan for `.kt`/`.kts` files (without it, the test is a
 *   no-op);
 * - `KOTLIN_STUB_BENCHMARK_LIMIT` — an optional cap on the number of files;
 * - `KOTLIN_STUB_BENCHMARK_VALIDATE` — when `true`, compares the stub trees built by the AST-based and light
 *   builders for every file instead of timing them.
 *
 * Example:
 * ```
 * KOTLIN_STUB_BENCHMARK_ROOT=~/work/dev ./gradlew :analysis:stubs:test --tests "*StubBuilderBenchmarkTest*"
 * ```
 */
class StubBuilderBenchmarkTest : AbstractAnalysisApiExecutionTest("testData/source/customData") {
    override val configurator: AnalysisApiTestConfigurator = LLSourceLikeTestConfigurator()

    @Test
    fun stubBenchmark(file: KtFile) {
        val root = System.getenv("KOTLIN_STUB_BENCHMARK_ROOT")?.ifEmpty { null } ?: run {
            println("KOTLIN_STUB_BENCHMARK_ROOT is not set, skipping the stub builder benchmark")
            return
        }

        val limit = System.getenv("KOTLIN_STUB_BENCHMARK_LIMIT")?.toIntOrNull() ?: Int.MAX_VALUE
        val validate = System.getenv("KOTLIN_STUB_BENCHMARK_VALIDATE")?.toBoolean() == true

        val files = collectFiles(Paths.get(root.replaceFirst("~", System.getProperty("user.home"))), limit)
        println("Collected ${files.size} Kotlin files under $root")

        val runner = BenchmarkRunner(file.project)
        if (validate) {
            runner.validateEquivalence(files)
        } else {
            runner.measure(files)
        }
    }

    private fun collectFiles(root: Path, limit: Int): List<Path> {
        val skippedDirectories = setOf("build", "out", "dist", ".git", "node_modules")
        Files.walk(root).use { stream ->
            return stream.asSequence()
                .filter { path -> Files.isRegularFile(path) }
                .filter { path -> path.extension == "kt" || path.extension == "kts" }
                .filterNot { path -> path.any { it.name in skippedDirectories } }
                .take(limit)
                .toList()
        }
    }
}

private class BenchmarkRunner(private val project: Project) {
    private val astBuilder = KtFileStubBuilder()

    private val lightBuilder: StubBuilder = StubElementRegistryService.getInstance()
        .getStubDescriptor(KotlinLanguage.INSTANCE)
        ?.stubDefinition
        ?.builder
        ?: error("No stub descriptor for Kotlin")

    /**
     * Creates a file whose AST is not yet parsed (`markAsCopy` would force the tree), so the light builder takes
     * the `parseContentsLight` path used by indexing.
     */
    private fun createKtFile(path: Path, text: String): KtFile =
        PsiFileFactory.getInstance(project).createFileFromText(
            /* name = */ path.name,
            /* fileType = */ KotlinFileType.INSTANCE,
            /* text = */ text,
            /* modificationStamp = */ 0,
            /* eventSystemEnabled = */ false,
            /* markAsCopy = */ false,
        ) as KtFile

    private fun readText(path: Path): String? = try {
        String(Files.readAllBytes(path), Charsets.UTF_8)
    } catch (_: Exception) {
        null
    }

    fun validateEquivalence(files: List<Path>) {
        var checked = 0
        val mismatches = mutableListOf<String>()
        val failures = mutableListOf<String>()

        for (path in files) {
            val text = readText(path) ?: continue
            try {
                // The light builder runs on a fresh unparsed file, taking the `parseContentsLight` path used by
                // indexing; the AST builder parses the file fully
                val lightFile = createKtFile(path, text)
                val lightStub = lightBuilder.buildStubTree(lightFile)
                check((lightFile.node as LazyParseableElement).isParsed.not()) {
                    "The light stub build must not expand the AST: $path"
                }

                val astStub = astBuilder.buildStubTree(createKtFile(path, text))

                val lightDump = DebugUtil.stubTreeToString(lightStub)
                val astDump = DebugUtil.stubTreeToString(astStub)
                if (lightDump != astDump) {
                    mismatches += path.toString()
                    if (mismatches.size <= 10) {
                        println("MISMATCH: $path")
                        printFirstDifference(astDump, lightDump)
                    }
                }
            } catch (e: Exception) {
                failures += "$path: $e"
                if (failures.size <= 10) {
                    println("FAILURE: $path")
                    e.printStackTrace()
                }
            }

            checked++
            if (checked % 20_000 == 0) {
                println("Checked $checked files, ${mismatches.size} mismatches, ${failures.size} failures")
            }
        }

        println("Done: checked $checked files, ${mismatches.size} mismatches, ${failures.size} failures")
        Assertions.assertTrue(mismatches.isEmpty() && failures.isEmpty()) {
            "Mismatches:\n${mismatches.take(50).joinToString("\n")}\nFailures:\n${failures.take(50).joinToString("\n")}"
        }
    }

    private fun printFirstDifference(expected: String, actual: String) {
        val expectedLines = expected.lines()
        val actualLines = actual.lines()
        for (index in 0 until maxOf(expectedLines.size, actualLines.size)) {
            val expectedLine = expectedLines.getOrNull(index)
            val actualLine = actualLines.getOrNull(index)
            if (expectedLine != actualLine) {
                println("  line ${index + 1}:")
                println("    ast:   $expectedLine")
                println("    light: $actualLine")
                break
            }
        }
    }

    fun measure(files: List<Path>) {
        val texts = ArrayList<Pair<Path, String>>(files.size)
        var totalBytes = 0L
        for (path in files) {
            val text = readText(path) ?: continue
            texts += path to text
            totalBytes += text.length
        }
        println("Loaded ${texts.size} files, ${totalBytes / 1024 / 1024} MB of text")

        // Warm up both paths on a prefix of the corpus
        val warmupTexts = texts.take(5_000)
        repeat(2) {
            for ([path, text] in warmupTexts) {
                try { astBuilder.buildStubTree(createKtFile(path, text)) } catch (_: Exception) { }
                try { lightBuilder.buildStubTree(createKtFile(path, text)) } catch (_: Exception) { }
            }
        }
        println("Warmup complete")

        repeat(3) { iteration ->
            var astNanos = 0L
            var lightNanos = 0L
            var parseNanos = 0L
            var astFailures = 0
            var lightFailures = 0

            for ([path, text] in texts) {
                // Full AST parse alone, for the breakdown; touching the first child expands the lazy file element
                val parseFile = createKtFile(path, text)
                val parseStart = System.nanoTime()
                try { parseFile.node.firstChildNode } catch (_: Exception) { }
                parseNanos += System.nanoTime() - parseStart

                val astFile = createKtFile(path, text)
                val astStart = System.nanoTime()
                try { astBuilder.buildStubTree(astFile) } catch (_: Exception) { astFailures++ }
                astNanos += System.nanoTime() - astStart

                val lightFile = createKtFile(path, text)
                val lightStart = System.nanoTime()
                try { lightBuilder.buildStubTree(lightFile) } catch (_: Exception) { lightFailures++ }
                lightNanos += System.nanoTime() - lightStart
            }

            fun ms(nanos: Long) = nanos / 1_000_000
            println(
                "Iteration ${iteration + 1}: " +
                        "full parse only: ${ms(parseNanos)} ms, " +
                        "AST stub build (parse + walk): ${ms(astNanos)} ms, " +
                        "light stub build: ${ms(lightNanos)} ms, " +
                        "speedup: ${"%.2f".format(astNanos.toDouble() / lightNanos)}x" +
                        (if (astFailures + lightFailures > 0) ", FAILURES: ast=$astFailures light=$lightFailures" else ""),
            )
        }
    }
}
