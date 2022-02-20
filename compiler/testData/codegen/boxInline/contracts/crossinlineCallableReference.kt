// !OPT_IN: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

import kotlin.contracts.*

interface SomeOutputScreenCallbacks {
    fun ontest()
}

class OutputWorkScreenView(callbacks: SomeOutputScreenCallbacks) {
    val root = vBox {
        button(callbacks::ontest)
    }
}

inline fun vBox(crossinline action: () -> Unit) {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return { action() }()
}

inline fun button(onAction: () -> Unit) {
    onAction()
}

// FILE: 2.kt

import test.*

fun box(): String {
    var res = "FAIL"
    OutputWorkScreenView(object : SomeOutputScreenCallbacks {
        override fun ontest() {
            res = "OK"
        }
    })
    return res
}
