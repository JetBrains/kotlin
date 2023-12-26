// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63877
// ISSUE: KT-50997 (multi-module variant)
// MODULE: lib
// FILE: lib.kt
typealias MySuspendFunction = suspend (String, String) -> Unit

fun foo(function: MySuspendFunction) {}

// MODULE: main(lib)
// FILE: main.kt
fun bar() = foo { a, b -> a + b }

fun box() = "OK"
