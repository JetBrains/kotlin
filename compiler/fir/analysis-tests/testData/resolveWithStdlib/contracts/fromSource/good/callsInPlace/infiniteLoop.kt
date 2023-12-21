// ISSUE: KT-64500
import kotlin.contracts.*

fun getCondition(): Boolean = true

@OptIn(ExperimentalContracts::class)
fun test_1(f: () -> Unit) {
    <!WRONG_INVOCATION_KIND!>contract { callsInPlace(f, InvocationKind.AT_LEAST_ONCE) }<!>
    while (true) {
        f()
    }
}

@OptIn(ExperimentalContracts::class)
fun test_2(f: () -> Unit) {
    contract { callsInPlace(f, InvocationKind.AT_LEAST_ONCE) }
    do {
        f()
    } while (true)
}

@OptIn(ExperimentalContracts::class)
fun test_3(f: () -> Unit) {
    <!WRONG_INVOCATION_KIND!>contract { callsInPlace(f, InvocationKind.AT_LEAST_ONCE) }<!>
    while (getCondition()) {
        f()
    }
}

@OptIn(ExperimentalContracts::class)
fun test_4(f: () -> Unit) {
    contract { callsInPlace(f, InvocationKind.AT_LEAST_ONCE) }
    do {
        f()
    } while (getCondition())
}

