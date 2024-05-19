// DUMP_CFG
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
inline fun inlineMultiple(block: () -> Unit) {
    contract {
        <!WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!>
    }
    block()
    block()
}

@OptIn(ExperimentalContracts::class)
fun multiple(block: () -> Unit) {
    contract {
        <!WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!>
    }
    block()
    block()
}

@OptIn(ExperimentalContracts::class)
inline fun inlineZero(block: () -> Unit) {
    contract {
        <!WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!>
    }
}

@OptIn(ExperimentalContracts::class)
fun zero(block: () -> Unit) {
    contract {
        <!WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!>
    }
}

fun test_1() {
    val x: Int
    inlineMultiple {
        x = 1
    }
}

fun test_2() {
    val x: Int
    multiple {
        x = 1
    }
}

fun test_3() {
    val x: Int
    inlineZero {
        x = 1
    }
}

fun test_4() {
    val x: Int
    zero {
        x = 1
    }
}
