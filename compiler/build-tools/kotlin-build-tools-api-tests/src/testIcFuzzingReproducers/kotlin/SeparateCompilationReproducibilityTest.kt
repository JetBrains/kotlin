/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.fuzzing

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * An inline function compiled together with its call site is inlined unoptimized, and optimized when it is read from a class file,
 * as in KT-19175. So an incremental build recompiling only the call site gives a different bytecode than a clean build,
 * although it recompiles everything it has to. These differences are excluded from the fuzzing by [model.KnownIssues.inlineOfSeparateCompilation].
 *
 * Each test builds `a.kt` with the inline functions and `b.kt` with their call sites, then touches `b.kt`,
 * so that the incremental build recompiles only `b.kt` and reads the inline functions from the class files.
 */
@DisplayName("The bytecode does not depend on whether the declarations it uses are compiled together with it")
class SeparateCompilationReproducibilityTest {
    private fun touchingCallSites(@Language("kotlin") inlineFunctions: String, @Language("kotlin") callSites: String) = icFuzzingReproducer {
        step("build", changed = mapOf("a.kt" to "package p0\n\n$inlineFunctions\n", "b.kt" to "package p0\n\n$callSites\n"))
        step("touch b.kt", touched = setOf("b.kt"))
    }

    // the $$forInline template of a suspend inline function keeps the unoptimized code of the inlined functions

    @Test
    @DisplayName("an inline function with an elvis operator calling the inline TODO")
    fun testInlineFunctionWithElvisCallingTodo() = touchingCallSites(
        """
            |fun v(): Long? = 18L
            |
            |inline fun f() {
            |    println(v() ?: TODO())
            |}
        """.trimMargin(),
        """
            |fun g() = f()
        """.trimMargin(),
    )

    @Test
    @DisplayName("an inline function with a redundant safe cast")
    fun testInlineFunctionWithRedundantSafeCast() = touchingCallSites(
        """
            |class C
            |
            |inline fun f() {
            |    println(C() as? C)
            |}
        """.trimMargin(),
        """
            |fun g() = f()
        """.trimMargin(),
    )

    @Test
    @DisplayName("an elvis operator calling the inline TODO on the result of an inline function with a when expression")
    fun testElvisCallingTodoOnResultOfInlineFunctionWithWhen() = touchingCallSites(
        """
            |inline fun f(): Array<Boolean>? {
            |    println(when (0) { 73 -> "c"; else -> "n" })
            |    return arrayOf(true)
            |}
        """.trimMargin(),
        """
            |fun g() = println(f() ?: TODO())
        """.trimMargin(),
    )

    @Test
    @DisplayName("a write to a @JvmField property of an object reads the object only when it is compiled together with it")
    fun testWriteToJvmFieldPropertyOfObject() = touchingCallSites(
        """
            |object O {
            |    @JvmField var p = 0
            |}
        """.trimMargin(),
        """
            |fun g() {
            |    O.p = 1
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("a reference to a @JvmField property of an object reads the object only when it is compiled together with it")
    fun testReferenceToJvmFieldPropertyOfObject() = touchingCallSites(
        """
            |object O {
            |    @JvmField var p = 0
            |}
        """.trimMargin(),
        """
            |fun g() = O::p
        """.trimMargin(),
    )

    @Test
    @DisplayName("a reference to a const property of an object has a line number only when the property is compiled together with it")
    fun testReferenceToConstPropertyOfObject() = touchingCallSites(
        """
            |object O {
            |    const val p = "s"
            |}
        """.trimMargin(),
        """
            |fun g() = O::p
        """.trimMargin(),
    )

    @Test
    @DisplayName("a reference to an inline function of a multifile class is owned by the part only when it is compiled together with it")
    fun testReferenceToInlineFunctionOfMultifileClass() = icFuzzingReproducer {
        step(
            "build",
            changed = mapOf(
                "a.kt" to "@file:JvmName(\"F\")\n@file:JvmMultifileClass\npackage p0\n\ninline fun f() = 0\n",
                "b.kt" to "package p0\n\nfun g() = run { val r: () -> Int = ::f; r }\n",
            ),
        )
        step("touch b.kt", touched = setOf("b.kt"))
    }

    @Test
    @DisplayName("a reference to a property of a multifile class is owned by the part only when it is compiled together with it")
    fun testReferenceToPropertyOfMultifileClass() = icFuzzingReproducer {
        step(
            "build",
            changed = mapOf(
                "a.kt" to "@file:JvmName(\"F\")\n@file:JvmMultifileClass\npackage p0\n\nval p: Any = 0\n",
                "b.kt" to "package p0\n\nfun g() = run { val r: kotlin.reflect.KProperty0<Any> = ::p; r }\n",
            ),
        )
        step("touch b.kt", touched = setOf("b.kt"))
    }

    @Test
    @DisplayName("a reference to a function of a multifile class is owned by the part only when it is compiled together with it")
    fun testReferenceToFunctionOfMultifileClass() = icFuzzingReproducer {
        step(
            "build",
            changed = mapOf(
                "a.kt" to "@file:JvmName(\"F\")\n@file:JvmMultifileClass\npackage p0\n\nfun f() = 0\n",
                "b.kt" to "package p0\n\nfun g() = run { val r: () -> Int = ::f; r }\n",
            ),
        )
        step("touch b.kt", touched = setOf("b.kt"))
    }

    @Test
    @DisplayName("a reference to a const property has a line number only when the property is compiled together with it")
    fun testReferenceToConstProperty() = touchingCallSites(
        """
            |const val p = "s"
        """.trimMargin(),
        """
            |fun f() = ::p
        """.trimMargin(),
    )

    @Test
    @DisplayName("an object implementing an interface with default members has a different constant pool order")
    fun testObjectImplementingInterfaceWithDefaultMembers() = touchingCallSites(
        """
            |interface C5 {
            |    val m6: Any?
            |        get() {
            |            return 53
            |        }
            |    fun m7(a0: Boolean = false): String {
            |        return "s13"
            |    }
            |}
        """.trimMargin(),
        """
            |object C9 : C5 {
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("a class implementing an interface with default members has a different member order")
    fun testClassImplementingInterfaceWithDefaultMembers() = touchingCallSites(
        """
            |interface C4 {
            |    val m5: Long
            |        get() {
            |            return 65L
            |        }
            |    fun m6(a0: Boolean = false, a1: Long = 0L): Long? {
            |        return 34L
            |    }
            |}
        """.trimMargin(),
        """
            |abstract class C7 : C4 {
            |    fun m8(): Int {
            |        return 16
            |    }
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("a file with inline functions compiled together with their call sites has a different order of its inner classes")
    fun testInlineFunctionsCompiledTogetherWithCallSites() = icFuzzingReproducer {
        step(
            "build",
            changed = mapOf(
                "a.kt" to "package p0\n",
                "b.kt" to """
                    |package p0
                    |
                    |inline fun f(): Any {
                    |    println(object {})
                    |    return 5
                    |}
                    |
                    |@JvmName("g")
                    |inline fun f(a: Array<Long>, block: () -> Int): suspend (Long) -> Boolean {
                    |    println(block())
                    |    println(object {})
                    |    return { _: Long -> false }
                    |}
                """.trimMargin(),
            ),
        )
        step("call f in a.kt", changed = mapOf("a.kt" to "package p0\n\nfun h() = f(arrayOf(0L)) { 0 }\n"))
    }

    @Test
    @DisplayName("an inline function called from a suspend inline function")
    fun testInlineFunctionCalledFromSuspendInlineFunction() = touchingCallSites(
        """
            |inline fun f(): String {
            |    println("x")
            |    return "a"
            |}
        """.trimMargin(),
        """
            |inline suspend fun g(): String {
            |    return f()
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("an inline function with a parameter called from a suspend inline function")
    fun testInlineFunctionWithParameterCalledFromSuspendInlineFunction() = touchingCallSites(
        """
            |inline fun f(a: String): String {
            |    println(a)
            |    return "a"
            |}
        """.trimMargin(),
        """
            |inline suspend fun g(): String {
            |    return f("s")
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("an inline function with a default value called from a suspend inline function")
    fun testInlineFunctionWithDefaultValueCalledFromSuspendInlineFunction() = touchingCallSites(
        """
            |inline fun f(a: String? = "s"): String {
            |    println(a)
            |    return "a"
            |}
        """.trimMargin(),
        """
            |inline suspend fun g(): String {
            |    return f()
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("an inline function with a noinline lambda parameter called from a suspend inline function")
    fun testNoinlineLambdaParameterInlinedIntoSuspendInlineFunction() = touchingCallSites(
        """
            |inline fun f(noinline block: () -> Int): String {
            |    println(block())
            |    return "a"
            |}
        """.trimMargin(),
        """
            |inline suspend fun g(): String {
            |    return f { 0 }
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("a suspend inline function with a default value called from a suspend inline function")
    fun testSuspendInlineFunctionWithDefaultValueCalledFromSuspendInlineFunction() = touchingCallSites(
        """
            |inline suspend fun f(a0: String? = "s0"): String {
            |    println(a0)
            |    return "a"
            |}
        """.trimMargin(),
        """
            |inline suspend fun g(): String {
            |    return f()
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("a suspend inline function with a local variable and a default value called from a suspend inline function")
    fun testSuspendInlineFunctionWithLocalVariableCalledFromSuspendInlineFunction() = touchingCallSites(
        """
            |inline suspend fun f(a0: String? = "s0"): String {
            |    val dead = false
            |    if (dead) println("d")
            |    return "a"
            |}
        """.trimMargin(),
        """
            |inline suspend fun g(): String {
            |    return f()
            |}
        """.trimMargin(),
    )

    // the code inlined through another inline function has different local variable slots

    @Test
    @DisplayName("an inline function with a default value and a noinline lambda parameter inlined through another inline function")
    fun testNoinlineLambdaParameterWithDefaultValueInlinedThroughAnotherInlineFunction() = touchingCallSites(
        """
            |inline fun f(a0: Long? = 0L, noinline block: () -> Int): Int {
            |    println(block())
            |    return 1
            |}
            |
            |inline fun g(): String {
            |    println(f(block = { 0 }))
            |    return "a"
            |}
        """.trimMargin(),
        """
            |fun h() {
            |    println(g())
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("an inline function with a parameter and a local variable inlined both directly and through another inline function")
    fun testLocalVariableInlinedDirectlyAndThroughAnotherInlineFunction() = touchingCallSites(
        """
            |inline fun f(a1: Long?): String {
            |    val dead = false
            |    if (dead) println("d")
            |    return "a"
            |}
            |
            |inline fun g(): Int {
            |    println(f(a1 = 0L))
            |    return 1
            |}
        """.trimMargin(),
        """
            |fun h() {
            |    println(f(a1 = 0L))
            |    println(g())
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("an inline function with a default value of a class type and a local variable inlined both directly and through another inline function")
    fun testLocalVariableWithDefaultValueInlinedDirectlyAndThroughAnotherInlineFunction() = touchingCallSites(
        """
            |open class C
            |
            |inline fun f(a0: C = C(), a1: Long?): String {
            |    val dead = false
            |    if (dead) println("d")
            |    return "a"
            |}
            |
            |inline fun g(): Int {
            |    println(f(a1 = 0L))
            |    return 1
            |}
        """.trimMargin(),
        """
            |fun h() {
            |    println(f(a1 = 0L))
            |    println(g())
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("an inline function with a default value and a lambda parameter inlined through another inline function")
    fun testLambdaParameterWithDefaultValueInlinedThroughAnotherInlineFunction() = touchingCallSites(
        """
            |inline fun f(a0: Long? = 0L, block: () -> Int): Long {
            |    println(block())
            |    return 1L
            |}
            |
            |inline fun g(block: () -> Int): Long {
            |    println(f { 0 })
            |    println(block())
            |    return 1L
            |}
        """.trimMargin(),
        """
            |fun h(): Int {
            |    println(g { 0 })
            |    return 11
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("an inline function with a parameter and a lambda parameter inlined both directly and through another inline function")
    fun testLambdaParameterAndParameterInlinedDirectlyAndThroughAnotherInlineFunction() = touchingCallSites(
        """
            |inline fun f(a0: Long?, block: () -> Int): Long {
            |    println(block())
            |    return 1L
            |}
            |
            |inline fun g(block: () -> Int): Long {
            |    println(f(0L) { 0 })
            |    println(block())
            |    return 1L
            |}
        """.trimMargin(),
        """
            |fun h(): Int {
            |    println(f(0L) { 0 })
            |    println(g { 0 })
            |    return 11
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("an inline function with a default value and a lambda parameter inlined both directly and through another inline function")
    fun testLambdaParameterWithDefaultValueInlinedDirectlyAndThroughAnotherInlineFunction() = touchingCallSites(
        """
            |inline fun f(a0: Long? = 0L, block: () -> Int): Long {
            |    println(block())
            |    return 1L
            |}
            |
            |inline fun g(a0: Long? = 0L, block: () -> Int): Long {
            |    println(f { 0 })
            |    println(block())
            |    return 1L
            |}
        """.trimMargin(),
        """
            |fun h(): Int {
            |    println(f { 0 })
            |    println(g { 0 })
            |    return 11
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("inline functions with inferred return types, default values and lambda parameters inlined into another inline function")
    fun testInferredReturnTypesInlinedIntoAnotherInlineFunction() = touchingCallSites(
        """
            |inline fun f4(a0: Long? = 0L, block: () -> Int) = run {
            |    println(block())
            |    println(object { override fun toString() = "o1" })
            |    1L
            |}
            |
            |inline fun f5(a0: Long? = 0L, block: () -> Int) = run {
            |    println(f4(block = { 0 }))
            |    println(block())
            |    println(object { override fun toString() = "o1" })
            |    1L
            |}
        """.trimMargin(),
        """
            |inline fun f6(crossinline block: () -> Int): Int {
            |    println(f4(block = { 0 }))
            |    println(f5(block = { 0 }))
            |    object : Runnable { override fun run() { println(block()) } }.run()
            |    return 11
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("an inline function with default values inlined through an inline function with a parameter into a suspend function")
    fun testDefaultValuesInlinedThroughInlineFunctionWithParameterIntoSuspendFunction() = touchingCallSites(
        """
            |inline fun f5(a0: Long? = 0L, a1: String = "s0") = run {
            |    1
            |}
            |
            |inline fun f8(a0: Boolean): Any {
            |    println(f5())
            |    return 55
            |}
        """.trimMargin(),
        """
            |suspend fun f14(a0: Any? = 0) = run {
            |    println(f8(a0 = false))
            |    val dead = false
            |    if (dead) println("d1")
            |    91
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("the fuzzing failure of an inline function with default values inlined through an inline function with a parameter")
    fun testFuzzingFailureOfDefaultValuesInlinedThroughInlineFunctionWithParameter() = icFuzzingReproducer {
        step(
            "build",
            changed = mapOf(
                "f1.kt" to """
                    |package p0
                    |
                    |inline fun f5(a0: Long? = 0L, a1: String = "s0") = run {
                    |    1
                    |}
                """.trimMargin(),
                "f2.kt" to """
                    |package p0
                    |
                    |import p1.C10
                    |import p1.f8
                    |
                    |val p13: Long = 84L
                    |
                    |suspend fun f14(a0: Any? = 0) = run {
                    |    println(C10)
                    |    println(f8(a0 = false))
                    |    val dead = false
                    |    if (dead) println("d1")
                    |    91
                    |}
                """.trimMargin(),
                "f3.kt" to """
                    |package p1
                    |
                    |typealias A6 = Boolean
                    |
                    |class C7 {
                    |    fun m9(): Int {
                    |        return 99
                    |    }
                    |}
                """.trimMargin(),
                "f4.kt" to """
                    |package p1
                    |
                    |import p0.f5
                    |
                    |inline fun f8(a0: A6): Any {
                    |    println(C7())
                    |    println(C7())
                    |    println(f5())
                    |    println(object { override fun toString() = "o1" })
                    |    return 55
                    |}
                    |
                    |object C10 {
                    |    fun m12(): A6 {
                    |        println(C7())
                    |        return false
                    |    }
                    |}
                """.trimMargin(),
            ),
        )
        step(
            "add a parameter to f14 in f2.kt",
            changed = mapOf(
                "f2.kt" to """
                    |package p0
                    |
                    |import p1.C10
                    |import p1.f8
                    |
                    |val p13: Long = 84L
                    |
                    |suspend fun f14(a0: Any? = 0, a1: Int? = 0) = run {
                    |    println(C10)
                    |    println(f8(a0 = false))
                    |    val dead = false
                    |    if (dead) println("d1")
                    |    91
                    |}
                """.trimMargin(),
            ),
        )
    }

    @Test
    @DisplayName("a reified inline function calling typeOf in a run lambda returning a class instance")
    fun testReifiedInlineFunctionCallingTypeOfInRunLambda() = touchingCallSites(
        """
            |inline fun <reified T> f6() = run {
            |    println(C4())
            |    println(kotlin.reflect.typeOf<T>())
            |    C4()
            |}
        """.trimMargin(),
        """
            |data class C4(val v: Int = 0)
            |
            |fun h(): Boolean {
            |    println(f6<Any>())
            |    return false
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("an inline function called through a contract smart cast in another inline function")
    fun testInlineFunctionCalledThroughContractSmartCastInInlineFunction() = touchingCallSites(
        """
            |@OptIn(kotlin.contracts.ExperimentalContracts::class)
            |inline fun f5(a0: String? = "s0", a1: String, block: () -> Int): Long {
            |    kotlin.contracts.contract { returns() implies (a0 != null) }
            |    println(block())
            |    return 42L
            |}
            |
            |inline fun f7(a0: Long = 0L) = run {
            |    println(run { val x: String? = null; f5(a0 = x, a1 = "s0", block = { 0 }); val y: String = x; y })
            |    8
            |}
        """.trimMargin(),
        """
            |suspend fun f8(a0: Any = 0): Any? {
            |    println(f7())
            |    return 29
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("an inline function with a run body called from a suspend function with a suspension point")
    fun testInlineFunctionWithRunBodyCalledFromSuspendFunction() = touchingCallSites(
        """
            |inline fun f7() = run {
            |    println(1)
            |    true
            |}
            |
            |suspend fun g(): Int = 1
        """.trimMargin(),
        """
            |suspend fun f8(): Int {
            |    println(f7())
            |    println(g())
            |    return 6
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("an inline property read in a when condition of an inline function")
    fun testInlinePropertyInWhenConditionOfInlineFunction() = touchingCallSites(
        """
            |inline val p: Boolean?
            |    get() = false
            |
            |inline fun f(): Int {
            |    println(when (false) { p -> "c"; else -> "n" })
            |    return 1
            |}
        """.trimMargin(),
        """
            |fun h(): Int {
            |    println(f())
            |    return 1
            |}
        """.trimMargin(),
    )

    @Test
    @DisplayName("an inline function reading an inline property of another file inlined into an extension function")
    fun testInlinePropertyInlinedThroughInlineFunction() = icFuzzingReproducer {
        step(
            "build",
            changed = mapOf(
                "f1.kt" to """
                    |package p0
                    |
                    |import p1.p4
                    |
                    |@JvmName("j7v1")
                    |inline fun <T : Any> f7(block: () -> Int) = run {
                    |    println(when (false) { p4 -> "c"; else -> "n" })
                    |    println(block())
                    |    println(object { override fun toString() = "o1" })
                    |    0
                    |}
                """.trimMargin(),
                "f2.kt" to """
                    |package p0
                    |
                    |sealed interface C11 {
                    |    val m13: Long
                    |}
                """.trimMargin(),
                "f3.kt" to """
                    |package p1
                    |
                    |inline var p4: Boolean?
                    |    get() = false
                    |    set(value) {}
                """.trimMargin(),
            ),
        )
        step(
            "add fun C11.f14 to f2.kt",
            changed = mapOf(
                "f2.kt" to """
                    |package p0
                    |
                    |sealed interface C11 {
                    |    val m13: Long
                    |}
                    |
                    |fun C11.f14(a0: Any = 0, a1: Boolean = false): Long? {
                    |    println(f7<Int>(block = { 0 }))
                    |    println(object { override fun toString() = "o1" })
                    |    return 13L
                    |}
                """.trimMargin(),
            ),
        )
    }

    @Test
    @DisplayName("a reified inline function with an inferred return type called from a companion object")
    fun testReifiedInlineFunctionCalledFromCompanionObject() = icFuzzingReproducer {
        step(
            "build",
            changed = mapOf(
                "f1.kt" to """
                    |package p0
                    |
                    |import p1.f6
                    |
                    |data class C4(val v: Int = 0) {
                    |    fun m11(): Long? {
                    |        println(f6<Any>())
                    |        return 99L
                    |    }
                    |    companion object C7 {
                    |        fun m8(a0: Boolean, a1: Boolean): Boolean {
                    |            println(f6<Any>())
                    |            return false
                    |        }
                    |        fun m9(a0: Boolean, a1: Boolean): Boolean {
                    |            return false
                    |        }
                    |    }
                    |}
                """.trimMargin(),
                "f3.kt" to """
                    |package p1
                    |
                    |import p0.C4
                    |
                    |inline fun <reified T> f6() = run {
                    |    println(C4())
                    |    println(kotlin.reflect.typeOf<T>())
                    |    println(object { override fun toString() = "o1" })
                    |    C4()
                    |}
                """.trimMargin(),
            ),
        )
        step(
            "move the first member of C7 in f1.kt to the end",
            changed = mapOf(
                "f1.kt" to """
                    |package p0
                    |
                    |import p1.f6
                    |
                    |data class C4(val v: Int = 0) {
                    |    fun m11(): Long? {
                    |        println(f6<Any>())
                    |        return 99L
                    |    }
                    |    companion object C7 {
                    |        fun m9(a0: Boolean, a1: Boolean): Boolean {
                    |            return false
                    |        }
                    |        fun m8(a0: Boolean, a1: Boolean): Boolean {
                    |            println(f6<Any>())
                    |            return false
                    |        }
                    |    }
                    |}
                """.trimMargin(),
            ),
        )
    }
}
