// ISSUE: KT-77713
// WITH_STDLIB
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// CHECK_BYTECODE_TEXT

// FILE: lib.kt
context(s: String)
fun foo() = s

fun bar(f: () -> String) = f()

inline fun baz(crossinline f: () -> String) = bar { f() }

// FILE: main.kt
context(_: String)
fun qux() = baz { foo() }

fun box() = context("OK") { qux() }


// 0 unused var\$inlined
