/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.fuzzing

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.tests.fuzzing.harness.IcFuzzHarness
import org.jetbrains.kotlin.buildtools.tests.fuzzing.harness.IcOracle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@DisplayName("The oracle of the incremental compilation fuzzing")
class IcOracleTest {
    @TempDir
    lateinit var workingDirectory: Path

    private val inlineFunction = "package p0\n\ninline fun f(): Int = 1\n"
    private val changedInlineFunction = "package p0\n\ninline fun f(): Int = 2\n"
    private val usage = "package p1\n\nimport p0.f\n\nfun g() = f()\n"

    private val interfaceWithDefaults = "package p0\n\ninterface I {\n    val p: Long\n        get() = 1L\n    fun m(a: Boolean = false): Long? = 2L\n}\n"
    private val implementation = "package p0\n\nabstract class C : I {\n    fun n(): Int = 3\n}\n"
    private val objectImplementation = "package p0\n\nobject C : I\n"

    @Test
    @DisplayName("accepts a correct incremental build")
    fun testCorrectIncrementalBuild() {
        IcFuzzHarness(workingDirectory).use { harness ->
            assertEquals(emptyList<String>(), IcOracle.check(harness.build(mapOf("f.kt" to inlineFunction, "g.kt" to usage))))
            assertEquals(emptyList<String>(), IcOracle.check(harness.build(mapOf("f.kt" to changedInlineFunction, "g.kt" to usage))))
            assertEquals(emptyList<String>(), IcOracle.check(harness.build(mapOf("f.kt" to changedInlineFunction))))
        }
    }

    @Test
    @DisplayName("detects stale outputs of an incremental build that missed a change")
    fun testMissedChange() {
        IcFuzzHarness(workingDirectory).use { harness ->
            harness.build(mapOf("f.kt" to inlineFunction, "g.kt" to usage))
            val violations = IcOracle.check(
                harness.build(
                    mapOf("f.kt" to changedInlineFunction, "g.kt" to usage),
                    sourcesChangesOverride = SourcesChanges.Known(modifiedFiles = emptyList(), removedFiles = emptyList()),
                )
            )
            assertTrue(violations.single().contains("Different: p0/FKt.class")) { violations.joinToString("\n") }
        }
    }

    @Test
    @DisplayName("detects a missed compilation error")
    fun testMissedError() {
        IcFuzzHarness(workingDirectory).use { harness ->
            harness.build(mapOf("f.kt" to inlineFunction, "g.kt" to usage))
            val violations = IcOracle.check(
                harness.build(
                    mapOf("f.kt" to "package p0\n", "g.kt" to usage),
                    sourcesChangesOverride = SourcesChanges.Known(modifiedFiles = emptyList(), removedFiles = emptyList()),
                )
            )
            assertTrue(violations.single().startsWith("The incremental build COMPILATION_SUCCESS, the rebuild COMPILATION_ERROR")) {
                violations.joinToString("\n")
            }
        }
    }

    @Test
    @DisplayName("accepts class files which differ only in the order of their members")
    fun testMemberOrder() = assertOnlyOrderDiffers(implementation)

    @Test
    @DisplayName("accepts class files which differ only in the order of their constant pool entries")
    fun testConstantPoolOrder() = assertOnlyOrderDiffers(objectImplementation)

    /**
     * The interface is read from its class file when only `c.kt` is recompiled, so the members of `C` delegating to its default members,
     * and their constants, are generated in another order: the class files differ, but the oracle accepts them.
     */
    private fun assertOnlyOrderDiffers(implementation: String) {
        IcFuzzHarness(workingDirectory).use { harness ->
            val sources = mapOf("i.kt" to interfaceWithDefaults, "c.kt" to implementation)
            harness.build(sources)
            val builds = harness.build(sources, touchedFiles = setOf("c.kt"))
            assertFalse(builds.incremental.outputs.getValue("p0/C.class").contentEquals(builds.rebuild.outputs.getValue("p0/C.class"))) {
                "The class files are expected to differ in the order of their members or constant pool entries"
            }
            assertEquals(emptyList<String>(), IcOracle.check(builds))
        }
    }
}
