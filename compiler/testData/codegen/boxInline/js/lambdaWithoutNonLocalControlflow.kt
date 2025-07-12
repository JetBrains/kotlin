// ISSUE: KT-68975
// See same test for diagnostics: compiler/testData/diagnostics/testsWithJsStdLibAndBackendCompilation/jsCode/lambdaWithoutNonLocalControlflow.kt
// TARGET_BACKEND: JS_IR
// WITH_STDLIB

// FILE: lib.kt
inline fun testLambdaInline(
    block: (Unit) -> String,
): String {
    return js("block()")
}

inline fun testLambdaNoInline(
    noinline block: (Unit) -> String,
): String {
    return js("block()")
}

inline fun testLambdaCrossInline(
    crossinline block: (Unit) -> String,
): String {
    return js("block()")
}

// FILE: main.kt
import kotlin.test.*

fun box(): String {
    assertEquals("OK", testLambdaInline { "OK" })
    assertEquals("OK", testLambdaNoInline { "OK" })
    assertEquals("OK", testLambdaCrossInline { "OK" })
    return "OK"
}
