// ISSUE: KT-68975
// See same test for diagnostics: compiler/testData/diagnostics/testsWithJsStdLibAndBackendCompilation/jsCode/lambdaWithoutNonLocalControlflow.kt
// TARGET_BACKEND: JS_IR
// WITH_STDLIB
// IGNORE_BACKEND_K2_MULTI_MODULE: JS_IR
// ^^^ KT-79680 `IrConstructorSymbolImpl is unbound` in lambdaWithoutNonLocalControlflow.kt

// FILE: lib.kt
import kotlin.test.*

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
fun box(): String {
    assertEquals("OK", testLambdaInline { "OK" })
    assertEquals("OK", testLambdaNoInline { "OK" })
    assertEquals("OK", testLambdaCrossInline { "OK" })
    return "OK"
}
