// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JS_IR

import kotlin.contracts.*

class A {
    val value = "Some value"

    init {
        foo {
            println(value)
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
    A()
    return "OK"
}