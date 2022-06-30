// !DUMP_CFG

import kotlin.contracts.*

fun unknown(x: () -> Unit) {
    contract { callsInPlace(x, InvocationKind.UNKNOWN) }
    x()
}

fun atLeastOnce(x: () -> Unit) {
    contract { callsInPlace(x, InvocationKind.AT_LEAST_ONCE) }
    x()
}

fun exactlyOnce(x: () -> Unit) {
    contract { callsInPlace(x, InvocationKind.AT_LEAST_ONCE) }
    x()
}

fun atMostOnce(x: () -> Unit) {
    contract { callsInPlace(x, InvocationKind.AT_MOST_ONCE) }
}

fun test1() {
    var x: Any?
    x = ""
    x.length
    unknown { x = 1 }
    x.<!UNRESOLVED_REFERENCE!>length<!>
    x.<!UNRESOLVED_REFERENCE!>inc<!>()
}

fun test2() {
    var x: Any?
    x = ""
    x.length
    atLeastOnce { x = 1 }
    x.<!UNRESOLVED_REFERENCE!>length<!>
    x.inc()
}

fun test3() {
    var x: Any?
    x = ""
    x.length
    exactlyOnce { x = 1 }
    x.<!UNRESOLVED_REFERENCE!>length<!>
    x.inc()
}

fun test4() {
    var x: Any?
    x = ""
    x.length
    atMostOnce { x = 1 }
    x.<!UNRESOLVED_REFERENCE!>length<!>
    x.<!UNRESOLVED_REFERENCE!>inc<!>()
}
