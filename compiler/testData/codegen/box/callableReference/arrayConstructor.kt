// WITH_STDLIB
// DISABLE_IR_TYPE_PARAMETER_SCOPE_CHECKS: ANY

// FILE: lib.kt

inline fun h(b: (Int, (Int) -> String) -> Array<String>): Array<String> =
    b(1) { "K" }

// FILE: main.kt

fun g(b: (Int, (Int) -> String) -> Array<String>): Array<String> =
    b(1) { "O" }

fun box(): String = g(::Array)[0] + h(::Array)[0]
