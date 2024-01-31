// ISSUE: KT-55096
// SKIP_TXT

import kotlin.contracts.*

class C(val x: Int)

@OptIn(ExperimentalContracts::class)
fun isNotNullAlsoCall(a: String?, b: () -> Unit): Boolean {
    contract {
        returns(true) implies (a != null)
        callsInPlace(b, InvocationKind.EXACTLY_ONCE)
    }
    b()
    return a != null
}

fun binaryBooleanExpression() {
    var x: String? = ""
    if (x is String || (x is String).also { x = null }) {
        x<!UNSAFE_CALL!>.<!>length // bad (x#0 is String, x#1 is Nothing?, this is either)
    }
}

fun unoverriddenEquals(a: Any?) {
    val c = C(1)
    var b: Any?
    b = c
    if (b == c.also { b = a }) {
        a.<!UNRESOLVED_REFERENCE!>x<!> // bad (b#0 is C, b#1 = a)
        b.<!UNRESOLVED_REFERENCE!>x<!> // bad (b#0 is C, this is b#1)
        if (b is C) { // b#1
            a.x // ok (b#1 = a)
            b.x // ok
        }
    }
}

fun safeCall() {
    var x: String? = ""
    if (x?.let { x = null; 1 } != null) {
        x<!UNSAFE_CALL!>.<!>length // bad (#2 != null => x#0 != null; but x#1 = null and this is either)
    }
}

fun contractFunction() {
    var x: String? = ""
    if (isNotNullAlsoCall(x) { x = null }) {
        x<!UNSAFE_CALL!>.<!>length // bad (#2 == true => x#0 != null; but this is x#1 = null)
    }
}
