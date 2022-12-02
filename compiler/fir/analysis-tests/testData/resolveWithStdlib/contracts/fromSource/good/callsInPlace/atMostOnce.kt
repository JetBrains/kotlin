// !DUMP_CFG

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
inline fun inlineRun(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    block()
}

@OptIn(ExperimentalContracts::class)
fun myRun(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    block()
}

fun test_1() {
    val x: Int
    inlineRun {
        x = 1
    }
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}

fun test_2() {
    val x: Int
    myRun {
        x = 1
    }
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}