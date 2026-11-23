/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.fuzzing

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Each test builds the sources, then changes `a.kt`, so that the incremental build has to recompile the files using it.
 */
@DisplayName("The incremental build recompiles everything a change affects")
class RecompilationTest {
    private fun changingA(sources: Map<String, String>, changedA: String) = icFuzzingReproducer {
        step("build", changed = sources.mapValues { "package p0\n\n${it.value}\n" })
        step("change a.kt", changed = mapOf("a.kt" to "package p0\n\n$changedA\n"))
    }

    @Test
    @DisplayName("on a new subclass of a sealed subclass of a sealed interface used in a when expression")
    fun testIndirectSealedSubclass() = changingA(
        mapOf(
            "a.kt" to "",
            "b.kt" to """
                |sealed interface S
                |
                |sealed class T : S
                |
                |class C : S
            """.trimMargin(),
            "c.kt" to """
                |fun f(s: S) = when (s) { is C -> 1 }
            """.trimMargin(),
        ),
        """
            |class D : T()
        """.trimMargin(),
    )

    @Test
    @DisplayName("on an abstract member added to a nested enum class")
    fun testEnumClassBecomingAbstract() = changingA(
        mapOf(
            "a.kt" to """
                |class O {
                |    enum class E { A }
                |}
            """.trimMargin(),
            "b.kt" to """
                |fun f() = O.E.entries
            """.trimMargin(),
        ),
        """
            |class O {
            |    enum class E {
            |        A { override fun g() = 0 };
            |        abstract fun g(): Int
            |    }
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("on a declaration-site variance making a use-site projection redundant")
    fun testDeclarationSiteVariance() = changingA(
        mapOf(
            "a.kt" to """
                |class G<T>
            """.trimMargin(),
            "b.kt" to """
                |interface I {
                |    val p: G<out Long> get() = TODO()
                |}
            """.trimMargin(),
            "c.kt" to """
                |interface J : I {
                |    fun g() = 0
                |}
            """.trimMargin(),
        ),
        """
            |class G<out T>
        """.trimMargin(),
    )

    @Test
    @DisplayName("on a nested class becoming open used in a member signature inherited by an interface in another file")
    fun testNestedClassBecomingOpenInInheritedSignature() = changingA(
        mapOf(
            "a.kt" to """
                |class O {
                |    class C
                |}
            """.trimMargin(),
            "b.kt" to """
                |interface I {
                |    fun m(c: O.C?) = 0
                |}
            """.trimMargin(),
            "c.kt" to """
                |interface J : I {
                |    fun g() = 0
                |}
            """.trimMargin(),
        ),
        """
            |class O {
            |    open class C
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("on a renamed abstract member of a fun interface used in a SAM conversion")
    fun testRenamedFunInterfaceMember() = changingA(
        mapOf(
            "a.kt" to """
                |fun interface S {
                |    fun m(): Int
                |}
            """.trimMargin(),
            "b.kt" to """
                |fun f() = S { 0 }
            """.trimMargin(),
        ),
        """
            |fun interface S {
            |    fun n(): Int
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("on @JvmDefaultWithoutCompatibility added to a value class calling an inherited default interface member")
    fun testValueClassBecomingJvmDefaultWithoutCompatibility() = changingA(
        mapOf(
            "a.kt" to """
                |@JvmInline
                |value class V(val v: Int) : I
            """.trimMargin(),
            "b.kt" to """
                |interface I {
                |    fun m() = 0
                |}
            """.trimMargin(),
            "c.kt" to """
                |fun f() = V(0).m()
            """.trimMargin(),
        ),
        """
            |@JvmDefaultWithoutCompatibility
            |@JvmInline
            |value class V(val v: Int) : I
        """.trimMargin(),
    )

    @Test
    @DisplayName("on a renamed abstract member of a fun interface used in a SAM conversion in an inline function")
    fun testRenamedFunInterfaceMemberInInlineFunction() = changingA(
        mapOf(
            "a.kt" to """
                |fun interface S {
                |    fun m(): Int
                |}
            """.trimMargin(),
            "b.kt" to """
                |inline fun f() = S { 0 }
            """.trimMargin(),
        ),
        """
            |fun interface S {
            |    fun n(): Int
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("on a member with a body added to a fun interface used in a SAM conversion in an inline function")
    fun testFunInterfaceMemberWithBodyInInlineFunction() = changingA(
        mapOf(
            "a.kt" to """
                |fun interface S {
                |    fun m(): Int
                |}
            """.trimMargin(),
            "b.kt" to """
                |inline fun f() = S { 0 }
            """.trimMargin(),
        ),
        """
            |fun interface S {
            |    fun m(): Int
            |    fun d(): Int = 1
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("on a value class becoming a regular class used as a receiver of an inline function")
    fun testValueClassBecomingRegularClass() = changingA(
        mapOf(
            "a.kt" to """
                |@JvmInline
                |value class C(val v: Int) {
                |    val p: Int get() = C(0).f()
                |}
            """.trimMargin(),
            "b.kt" to """
                |inline fun C.f(): Int = 0
            """.trimMargin(),
        ),
        """
            |abstract class C {
            |    val p: Int get() = (TODO() as C).f()
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("on a renamed file with an inline member function")
    fun testRenamedFileWithInlineMemberFunction() = icFuzzingReproducer {
        step(
            "build",
            changed = mapOf(
                "a.kt" to "package p0\n\nclass C {\n    inline fun f() = println()\n}\n",
                "b.kt" to "package p0\n\nfun g() = C().f()\n",
            ),
        )
        step("rename a.kt to c.kt", changed = mapOf("c.kt" to "package p0\n\nclass C {\n    inline fun f() = println()\n}\n"), removed = setOf("a.kt"))
    }

    @Test
    @DisplayName("on a renamed file with a JVM name and a top-level inline function")
    fun testRenamedFileWithJvmNameAndInlineFunction() = icFuzzingReproducer {
        val a = "@file:JvmName(\"F\")\npackage p0\n\ninline fun f() = println()\n"
        step("build", changed = mapOf("a.kt" to a, "b.kt" to "package p0\n\nfun g() = f()\n"))
        step("rename a.kt to c.kt", changed = mapOf("c.kt" to a), removed = setOf("a.kt"))
    }

    @Test
    @DisplayName("on a class with an inline member function moved to another file")
    fun testMovedClassWithInlineMemberFunction() = icFuzzingReproducer {
        step(
            "build",
            changed = mapOf(
                "a.kt" to "package p0\n\nclass C {\n    inline fun f() = println()\n}\n",
                "b.kt" to "package p0\n\nfun g() = C().f()\n",
                "c.kt" to "package p0\n",
            ),
        )
        step(
            "move C from a.kt to c.kt",
            changed = mapOf("a.kt" to "package p0\n", "c.kt" to "package p0\n\nclass C {\n    inline fun f() = println()\n}\n"),
        )
    }
}
