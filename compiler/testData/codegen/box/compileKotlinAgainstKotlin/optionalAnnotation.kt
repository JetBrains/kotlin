// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE
// FIR status: expect/actual in the same module (ACTUAL_WITHOUT_EXPECT)
// !LANGUAGE: +MultiPlatformProjects
// !OPT_IN: kotlin.ExperimentalMultiplatform
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// MODULE: lib
// FILE: A.kt
package a

@OptionalExpectation
expect annotation class A(val x: Int)

@OptionalExpectation
expect annotation class B(val s: String)

actual annotation class A(actual val x: Int)

// MODULE: main(lib)
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

    try {
        Class.forName("a.B")
        return "Fail 2: there should be no class file for a.B"
    } catch (e: ClassNotFoundException) {
        return "OK"
    }
}
