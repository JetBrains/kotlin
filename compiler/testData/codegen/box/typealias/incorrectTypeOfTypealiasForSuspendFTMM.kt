// JVM_ABI_K1_K2_DIFF: KT-68087
// WITH_STDLIB
// ISSUE: KT-50997 (multi-module variant)
// MODULE: lib
// FILE: lib.kt
typealias MySuspendFunction = suspend (String, String) -> Unit

fun foo(function: MySuspendFunction) {}

// MODULE: main(lib)
// FILE: main.kt
fun bar() = foo { a, b -> a + b }

fun box() = "OK"
