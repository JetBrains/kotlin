// MODULE: m1
// FILE: a.kt

package test

enum class E {
    FIRST
}

sealed class S

class Derived : S()

// MODULE: m2(m1)
// FILE: b.kt

package other

import test.*

fun foo(e: E) = when (e) {
    E.FIRST -> 42
    else -> -42
}

fun bar(s: S?) = when (s) {
    is Derived -> "Derived"
    null -> ""
    else -> TODO("What?!?!")
}

fun baz(b: Boolean?) = when (b) {
    true -> 1
    false -> 0
    null -> -1
    // Still warning
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> TODO()
}

fun baz(b: Boolean) = when (b) {
    true -> 1
    false -> 0
    // Still warning
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> TODO()
}