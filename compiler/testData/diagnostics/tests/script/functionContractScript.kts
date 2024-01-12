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
exactlyOnce { <!CAPTURED_MEMBER_VAL_INITIALIZATION!>a<!> = "" }
a.length

val b: String
atLeastOnce { <!VAL_REASSIGNMENT!>b<!> = "" }
b.length

<!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val c: String<!>
atMostOnce { <!CAPTURED_MEMBER_VAL_INITIALIZATION!>c<!> = "" }
<!UNINITIALIZED_VARIABLE!>c<!>.length
