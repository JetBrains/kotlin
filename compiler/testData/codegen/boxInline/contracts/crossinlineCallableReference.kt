// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect +ReadDeserializedContracts
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

import kotlin.contracts.*

interface SomeOutputScreenCallbacks {
    fun ontest()
}

@ExperimentalContracts
class OutputWorkScreenView(callbacks: SomeOutputScreenCallbacks) {
    val root = vBox {
        button(callbacks::ontest)
    }
}

@ExperimentalContracts
inline fun vBox(
    crossinline action: () -> Unit
) {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return { action() }()
}

@ExperimentalContracts
inline fun button(
    onAction: () -> Unit
) {
    onAction()
}

// FILE: 2.kt

import test.*
import kotlin.contracts.*

@ExperimentalContracts
fun box(): String {
    var res = "FAIL"
    OutputWorkScreenView(object : SomeOutputScreenCallbacks {
        override fun ontest() {
            res = "OK"
        }
    })
    return res
}