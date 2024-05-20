// DUMP_CFG
// WITH_STDLIB
// ISSUE: KT-68291
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun zero(block: () -> Unit) {
    contract {
        <!WRONG_INVOCATION_KIND("block: () -> Unit; EXACTLY_ONCE; AT_MOST_ONCE")!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!>
    }
}

@OptIn(ExperimentalContracts::class)
fun zero2(block: () -> Unit) {
    contract {
        <!WRONG_INVOCATION_KIND("block: () -> Unit; AT_LEAST_ONCE; AT_MOST_ONCE")!>callsInPlace(block, InvocationKind.AT_LEAST_ONCE)<!>
    }
}

@OptIn(ExperimentalContracts::class)
fun multiple(block: () -> Unit) {
    contract {
        <!WRONG_INVOCATION_KIND("block: () -> Unit; EXACTLY_ONCE; AT_LEAST_ONCE")!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!>
    }

    block()
    block()
}

@OptIn(ExperimentalContracts::class)
fun multiple2(block: () -> Unit) {
    contract {
        <!WRONG_INVOCATION_KIND("block: () -> Unit; AT_MOST_ONCE; AT_LEAST_ONCE")!>callsInPlace(block, InvocationKind.AT_MOST_ONCE)<!>
    }

    block()
    block()
}
