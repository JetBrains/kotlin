// !OPT_IN: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: JVM
// WITH_STDLIB
// KT-38849

import kotlin.contracts.*

fun block(lambda: () -> Unit) {
    contract {
        callsInPlace(lambda, InvocationKind.EXACTLY_ONCE)
    }
    lambda()
}

fun box(): String {
    val list: List<Int>

    block {
        list = listOf(1, 2, 3)
    }

    block {
        if (listOf(2, 3, 4).first { list.contains(it) } != 2) throw AssertionError("Fail")
    }

    return "OK"
}
