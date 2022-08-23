// WITH_STDLIB
// ISSUE: KT-50997
// MODULE: lib
// FILE: lib.kt
typealias MySuspendFunction = suspend (String, String) -> Unit

fun foo(function: MySuspendFunction) {}

// MODULE: main(lib)
// FILE: main.kt
fun bar() = foo { a, b -> a + b }

fun box() = "OK"
