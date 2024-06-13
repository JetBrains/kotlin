// ISSUE: KT-68975
// See same test for diagnostics: compiler/testData/diagnostics/testsWithJsStdLibAndBackendCompilation/jsCode/inlinedReturnBreakContinue/nonLocalReturn.kt
// TARGET_BACKEND: JS
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// REASON: IllegalStateException: unreachable: OK

inline fun foo(makeInt: () -> Int): Int {
    return js("makeInt()")
}

fun box(): String {
    val i: Int = foo {
        return "OK"
    }
    error("unreachable: $i")
}
