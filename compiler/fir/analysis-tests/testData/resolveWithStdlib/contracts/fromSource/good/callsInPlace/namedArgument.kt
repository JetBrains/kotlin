// ISSUE: KT-64501
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
inline fun myRun(f: () -> Unit) {
    contract { callsInPlace(f, InvocationKind.EXACTLY_ONCE) }
    f()
}

@OptIn(ExperimentalContracts::class)
inline fun test_1(g: () -> Unit) {
    contract { callsInPlace(g, InvocationKind.EXACTLY_ONCE) }
    myRun(f = g)
}

@OptIn(ExperimentalContracts::class)
inline fun test_2(g: () -> Unit) {
    contract { callsInPlace(g, InvocationKind.EXACTLY_ONCE) }
    myRun(g)
}
