// !DUMP_CFG
// !OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun <T> unknown(x: () -> T): T {
    contract { callsInPlace(x, InvocationKind.UNKNOWN) }
    return x()
}

fun <T> atLeastOnce(x: () -> T): T {
    contract { callsInPlace(x, InvocationKind.AT_LEAST_ONCE) }
    return x()
}

fun <T> exactlyOnce(x: () -> T): T {
    contract { callsInPlace(x, InvocationKind.EXACTLY_ONCE) }
    return x()
}

fun <T> atMostOnce(x: () -> T): T {
    contract { callsInPlace(x, InvocationKind.AT_MOST_ONCE) }
    return x()
}

fun <T> noContract(x: () -> T): T = x()

fun <K> select(vararg x: K): K = x[0]
fun <T> id(x: T): T = x
fun <K> materialize(): K = null!!

fun basic(x: Any?) {
    exactlyOnce { x as Int }
    x.inc() // OK
}

fun completedCallExactlyOnce(x: Any?, y: Any?) {
    select(
        // The value of the type argument is known, so the call is complete and the data can flow.
        id(exactlyOnce { y.<!UNRESOLVED_REFERENCE!>inc<!>(); x as Int }),
        y as Int,
        exactlyOnce { x.<!UNRESOLVED_REFERENCE!>inc<!>(); y.inc(); 1 }
    ).inc() // OK
    x.inc() // OK
    y.inc() // OK
}

fun completedCallAtLeastOnce(x: Any?, y: Any?) {
    select(
        id(atLeastOnce { y.<!UNRESOLVED_REFERENCE!>inc<!>(); x as Int }),
        y as Int,
        atLeastOnce { x.<!UNRESOLVED_REFERENCE!>inc<!>(); y.inc(); 1 }
    ).inc() // OK
    x.inc() // OK
    y.inc() // OK
}

fun completedCallAtMostOnce(x: Any?, y: Any?) {
    select(
        id(atMostOnce { y.<!UNRESOLVED_REFERENCE!>inc<!>(); x as Int }),
        y as Int,
        atMostOnce { x.<!UNRESOLVED_REFERENCE!>inc<!>(); y.inc(); 1 }
    ).inc() // OK
    x.<!UNRESOLVED_REFERENCE!>inc<!>() // Bad: x as Int might not have executed
    y.inc() // OK
}

fun completedCallUnknown(x: Any?, y: Any?) {
    select(
        id(unknown { y.<!UNRESOLVED_REFERENCE!>inc<!>(); x as Int }),
        y as Int,
        unknown { x.<!UNRESOLVED_REFERENCE!>inc<!>(); y.inc(); 1 }
    ).inc() // OK
    x.<!UNRESOLVED_REFERENCE!>inc<!>() // Bad: x as Int might not have executed
    y.inc() // OK
}

fun completedCallNoContract(x: Any, y: Any) {
    select(
        id(noContract { y.<!UNRESOLVED_REFERENCE!>inc<!>(); x as Int }),
        y as Int,
        noContract { x.<!UNRESOLVED_REFERENCE!>inc<!>(); y.inc(); 1 }
    ).inc() // OK
    x.<!UNRESOLVED_REFERENCE!>inc<!>() // Bad: x as Int might not have executed
    y.inc() // OK
}

fun incompleteCallExactlyOnce(x: Any, y: Any) {
    select(
        // The type argument is uninferred, so the two lambdas are concurrent by data flow.
        id(exactlyOnce { x as Int; y.<!UNRESOLVED_REFERENCE!>inc<!>(); x.inc(); materialize() }),
        exactlyOnce { y as Int; x.<!UNRESOLVED_REFERENCE!>inc<!>(); y.inc(); 1 }
    ).inc() // OK
    x.inc() // OK
    y.inc() // OK
}

fun incompleteCallAtLeastOnce(x: Any, y: Any) {
    select(
        id(atLeastOnce { x as Int; y.<!UNRESOLVED_REFERENCE!>inc<!>(); x.inc(); materialize() }),
        atLeastOnce { y as Int; x.<!UNRESOLVED_REFERENCE!>inc<!>(); y.inc(); 1 }
    ).inc() // OK
    x.inc() // OK
    y.inc() // OK
}

fun incompleteCallAtMostOnce(x: Any, y: Any) {
    select(
        id(atMostOnce { x as Int; y.<!UNRESOLVED_REFERENCE!>inc<!>(); x.inc(); materialize() }),
        atMostOnce { y as Int; x.<!UNRESOLVED_REFERENCE!>inc<!>(); y.inc(); 1 }
    ).inc() // OK
    x.<!UNRESOLVED_REFERENCE!>inc<!>() // Bad: x as Int might not have executed
    y.<!UNRESOLVED_REFERENCE!>inc<!>() // Bad: y as Int might not have executed
}

fun incompleteCallUnknown(x: Any, y: Any) {
    select(
        id(unknown { x as Int; y.<!UNRESOLVED_REFERENCE!>inc<!>(); x.inc(); materialize() }),
        unknown { y as Int; x.<!UNRESOLVED_REFERENCE!>inc<!>(); y.inc(); 1 }
    ).inc() // OK
    x.<!UNRESOLVED_REFERENCE!>inc<!>() // Bad: x as Int might not have executed
    y.<!UNRESOLVED_REFERENCE!>inc<!>() // Bad: y as Int might not have executed
}

fun incompleteCallNoContract(x: Any, y: Any) {
    select(
        id(noContract { x as Int; y.<!UNRESOLVED_REFERENCE!>inc<!>(); x.inc(); materialize() }),
        noContract { y as Int; x.<!UNRESOLVED_REFERENCE!>inc<!>(); y.inc(); 1 }
    ).inc() // OK
    x.<!UNRESOLVED_REFERENCE!>inc<!>() // Bad: x as Int might not have executed
    y.<!UNRESOLVED_REFERENCE!>inc<!>() // Bad: y as Int might not have executed
}

fun expectedType() {
    val x: Int = select(run { materialize() }, run { materialize() })
    x.inc()
}

fun expectedTypeNested() {
    val x: Int = id(noContract { run { materialize() } })
    x.inc()
}
