// !LANGUAGE: +MultiPlatformProjects
// !USE_EXPERIMENTAL: kotlin.ExperimentalMultiplatform
// IGNORE_BACKEND: NATIVE
// FULL_JDK
// FILE: A.kt
package a

@OptionalExpectation
expect annotation class A(val x: Int)

@OptionalExpectation
expect annotation class B(val s: String)

actual annotation class A(actual val x: Int)

// FILE: B.kt

@file:Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE") // TODO: support common sources in the test infrastructure

import a.A
import a.B
import java.lang.reflect.Modifier

class Test {
    @A(42)
    @B("OK")
    fun test() {}
}

fun box(): String {
    val annotations = Test::class.java.declaredMethods.single().annotations.toList()
    if (annotations.toString() != "[@a.A(x=42)]") return "Fail 1: $annotations"

    // Can't use B::class.java because "Declaration annotated with '@OptionalExpectation' can only be used inside an annotation entry"
    if (Modifier.isPublic(Class.forName("a.B").modifiers)) return "Fail 2: optional annotation class should not be public in the bytecode"

    return "OK"
}
