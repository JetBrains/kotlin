// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect +ReadDeserializedContracts
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: JVM_IR, NATIVE
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR
// FILE: 1.kt
package test

import kotlin.contracts.*

@ExperimentalContracts
class A {
    var res = "FAIL"

    fun foo() {
        bar {
            res = "OK"
        }
    }

    inline fun bar(crossinline not_exactly_once: () -> Unit) {
        baz {
            not_exactly_once()
        }
    }
}

@ExperimentalContracts
inline fun baz(crossinline exactly_once: () -> Unit) {
    contract {
        callsInPlace(exactly_once, InvocationKind.EXACTLY_ONCE)
    };

    { exactly_once() }()
}

// FILE: 2.kt

import test.*

fun box(): String {
    val a = A()
    a.foo()
    return a.res
}
