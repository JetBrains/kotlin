// !OPT_IN: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE
// WITH_STDLIB

import kotlin.contracts.*

class A {
    val value = arrayListOf("O")

    init {
        foo {
            value += "K"
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
    return if (a.value == listOf("O", "K"))  "OK" else "FAIL"
}