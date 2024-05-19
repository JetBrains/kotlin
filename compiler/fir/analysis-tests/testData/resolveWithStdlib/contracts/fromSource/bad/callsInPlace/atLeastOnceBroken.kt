// DUMP_CFG
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
inline fun inlineZero(block: () -> Unit) {
    contract {
        <!WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.AT_LEAST_ONCE)<!>
    }
}

@OptIn(ExperimentalContracts::class)
fun zero(block: () -> Unit) {
    contract {
        <!WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.AT_LEAST_ONCE)<!>
    }
}

fun test_1() {
    val x: Int
    inlineZero {
        <!VAL_REASSIGNMENT!>x<!> = 1
    }
}

fun test_2() {
    val x: Int
    zero {
        <!VAL_REASSIGNMENT!>x<!> = 1
    }
}
