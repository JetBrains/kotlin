// DUMP_CFG
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
inline fun inlineMultiple(block: () -> Unit) {
    contract {
        <!WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.AT_MOST_ONCE)<!>
    }

    block()
    block()
}

@OptIn(ExperimentalContracts::class)
fun multiple(block: () -> Unit) {
    contract {
        <!WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.AT_MOST_ONCE)<!>
    }

    block()
    block()
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
