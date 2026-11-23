/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.fuzzing

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * A diagnostic listing declarations lists them in a different order when they are read from a class file.
 * These differences are excluded from the fuzzing by [model.KnownIssues.classFileOrder].
 * An import of an enum entry with a body nested in a private class of another file is resolved to the class of the entry
 * when the entry is read from a class file, excluded by [model.KnownIssues.importOfPrivateEnumEntryClass].
 *
 * Each test builds `a.kt` with the declarations and `b.kt` using them, then breaks `b.kt`,
 * so that the incremental build recompiles only `b.kt` and reads the declarations from the class files.
 */
@DisplayName("The diagnostics do not depend on whether the declarations they list are compiled together with them")
class DiagnosticsReproducibilityTest {
    private fun breakingB(@Language("kotlin") a: String, @Language("kotlin") b: String, @Language("kotlin") brokenB: String) = icFuzzingReproducer {
            step("build", changed = mapOf("a.kt" to "package p0\n\n$a\n", "b.kt" to "package p0\n\n$b\n"))
            step("break b.kt", changed = mapOf("b.kt" to "package p0\n\n$brokenB\n"))
        }

    @Test
    @DisplayName("the members a class does not implement")
    fun testNotImplementedMembers() = breakingB(
        """
            |abstract class B {
            |    abstract val p: Int
            |    abstract fun m(): Int
            |}
        """.trimMargin(),
        """
            |class C : B() {
            |    override val p = 0
            |    override fun m() = 0
            |}
        """.trimMargin(),
        """
            |class C : B()
        """.trimMargin(),
    )

    @Test
    @DisplayName("the candidates of an inapplicable call")
    fun testInapplicableCandidates() = breakingB(
        """
            |fun f(a: Int) = 0
        """.trimMargin(),
        """
            |fun f(a: String) = 0
            |
            |fun g() = f(1)
        """.trimMargin(),
        """
            |fun f(a: String) = 0
            |
            |fun g() = f(true)
        """.trimMargin(),
    )

    @Test
    @DisplayName("an imported enum entry with a body nested in a private class of another file")
    fun testImportedEnumEntryWithBodyNestedInPrivateClass() = breakingB(
        """
            |private interface I {
            |    enum class E { A { override fun toString() = "" } }
            |}
        """.trimMargin(),
        "",
        """
            |import p0.I.E.A
            |
            |fun f() = println(A)
        """.trimMargin(),
    )
}
