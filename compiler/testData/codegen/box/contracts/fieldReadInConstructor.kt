// !OPT_IN: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE
// WITH_STDLIB

import kotlin.contracts.*

var x = ""

fun baz(s: String) { x += s }

class A {
    val value = "Some value"

    init {
        foo {
            baz(value)
        }
    }
}

fun foo(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun box(): String {
    val a = A()
    if (x != a.value) return "FAIL: $x"
    return "OK"
}