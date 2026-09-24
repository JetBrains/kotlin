/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.tests.compilation.jps

import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.JvmJpsManagedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.absolutePathString

@DisplayName("The JPS expect/actual tracker")
class ExpectActualTrackerJpsTest : BaseJpsTest() {

    @DisplayName("A matched expect/actual pair is reported with both source files")
    @TestMetadata("expect-actual-metadata")
    @Test
    fun matchedExpectActualIsReported() {
        val fixture = "expect-actual-metadata"
        jvmProject(inProcess) {
            val module = module(fixture, moduleCompilationConfigAction = configureKmpJvmFragments())
            val expectActualTracker = RecordingExpectActualTracker()
            module.compile(compilationConfigAction = { builder ->
                builder.withJpsIc {
                    this[JvmJpsManagedIncrementalCompilationConfiguration.EXPECT_ACTUAL_TRACKER] = expectActualTracker
                }
            }) {
                val matched = expectActualTracker.matched
                    .map { [expectFilePath, actualFilePath] -> expectFilePath.relativeToModule(fixture) to actualFilePath.relativeToModule(fixture) }
                    .toSet()
                assertEquals(
                    setOf("/src/commonMain/expectFoo.kt" to "/src/jvmMain/actualFoo.kt"),
                    matched,
                ) { "unexpected expect/actual reports" }
                assertTrue(expectActualTracker.lenientStubs.isEmpty()) {
                    "reportExpectOfLenientStub must not fire when every expect has an actual; " +
                            "got ${expectActualTracker.lenientStubs}"
                }
            }
        }
    }
}

// A deliberate private copy of the identically named helper in
// `src/testClasspathMetadata/kotlin/ClasspathMetadataIncrementalTest.kt`, so that this change stays confined to the
// new suite. De-duplicating the two into `src/main` is a valid follow-up.
private fun configureKmpJvmFragments(): (JvmCompilationOperation.Builder) -> Unit = { builder ->
    val fragmentHierarchy = listOf("commonMain", "intermediateMain", "jvmMain")
    fun fragmentOf(path: Path): String? = fragmentHierarchy.firstOrNull { fragment -> path.any { it.toString() == fragment } }

    val sourcesByFragment = builder.sources
        .mapNotNull { source -> fragmentOf(source)?.let { fragment -> fragment to source } }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })

    val fragments = fragmentHierarchy.filter { it in sourcesByFragment }
    val fragmentSources = fragments.flatMap { fragment ->
        sourcesByFragment.getValue(fragment).map { "$fragment:${it.absolutePathString()}" }
    }
    val fragmentRefines = fragments.zipWithNext { parent, child -> "$child:$parent" }

    val args = buildList {
        add("-Xmulti-platform")
        add("-Xfragments=${fragments.joinToString(",")}")
        if (fragmentRefines.isNotEmpty()) {
            add("-Xfragment-refines=${fragmentRefines.joinToString(",")}")
        }
        add("-Xfragment-sources=${fragmentSources.joinToString(",")}")
        add("-Xuse-metadata-on-incremental-classpath=false")
    }

    builder.compilerArguments.applyCommandLineArguments(args)
}
