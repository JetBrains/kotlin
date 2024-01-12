// !OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

inline fun <T> exactlyOnce(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block()
}

inline fun <T> atLeastOnce(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.AT_LEAST_ONCE) }
    return block()
}

inline fun <T> atMostOnce(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
    return block()
}

val a: String
exactlyOnce { <!VAL_REASSIGNMENT!>a<!> = "" }
a.length

val b: String
atLeastOnce { <!VAL_REASSIGNMENT, VAL_REASSIGNMENT!>b<!> = "" }
b.length

<!MUST_BE_INITIALIZED!>val c: String<!>
atMostOnce { <!VAL_REASSIGNMENT!>c<!> = "" }
<!UNINITIALIZED_VARIABLE!>c<!>.length
