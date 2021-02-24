// !DUMP_CFG

import kotlin.contracts.*

inline fun inlineRun(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun myRun(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun test_1() {
    val x: Int
    inlineRun {
        x = 1
    }
    x.inc()
}

fun test_2() {
    val x: Int
    myRun {
        x = 1
    }
    x.inc()
}