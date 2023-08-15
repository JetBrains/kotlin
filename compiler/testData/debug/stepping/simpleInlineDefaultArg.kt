// IGNORE_BACKEND: WASM
// FILE: test.kt

inline fun alsoInline() = "OK"

inline fun ifoo(s: String = alsoInline()): String {
    return s
}

fun box(): String {
    return ifoo()
}

// `alsoInline` is inlined in the `ifoo$default` stub. When exiting the inline
// codegen, the line number is reset. Therefore any code generated with line
// number information will emit a new line number so that stepping continues
// in the caller.
//
// For JVM none of the code after `alsoInline` leads to line number generation.
// This seems problematic as the line number for the inline function is then
// in effect for instructions outside the scope of the `$i$f$alsoInline`
// variable life time.
//
// For JVM_IR a line number is generated for the code following `alsoInline`.
// Therefore, we stop on line 5 before entering `alsoInline`
// and we step back to line 5 after `alsoInline` and before the body of
// `ifoo`. This seems reasonable and also help ensure that line numbers
// for the inline function are only used for instructions covered by
// the `$i$f$alsoInline` variable life time.

// EXPECTATIONS JVM JVM_IR
// test.kt:11 box
// test.kt:6 box
// test.kt:4 box
// EXPECTATIONS JVM_IR
// test.kt:6 box
// EXPECTATIONS JVM JVM_IR
// test.kt:7 box
// test.kt:11 box

// EXPECTATIONS JS_IR
// test.kt:11 box
