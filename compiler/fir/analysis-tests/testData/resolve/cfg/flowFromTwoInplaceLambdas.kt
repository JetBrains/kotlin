// !DUMP_CFG
import kotlin.contracts.*

fun <T> n(): T? = null

@OptIn(ExperimentalContracts::class)
fun <T> run2(x: () -> T, y: () -> T) {
    contract {
        callsInPlace(x, InvocationKind.EXACTLY_ONCE)
        callsInPlace(y, InvocationKind.EXACTLY_ONCE)
    }
    x()
    y()
}

fun test1(x: String?) {
    var p = x
    if (p != null) {
        run2(
            { p = null; n() },
            { <!SMARTCAST_IMPOSSIBLE!>p<!>.length; 123 } // Bad: may or may not not be called first
        )
        p<!UNSAFE_CALL!>.<!>length // Bad: p = null
    }
}

fun test1_tail(x: String?) {
    var p = x
    if (p != null) {
        run2({ p = null; n() }) {
            <!SMARTCAST_IMPOSSIBLE!>p<!>.length // Bad: may or may not not be called first
            123
        }
        p<!UNSAFE_CALL!>.<!>length // Bad: p = null
    }
}

fun test2(x: Any?) {
    var p: Any? = x
    p.<!UNRESOLVED_REFERENCE!>length<!> // Bad
    run2({ p = null; n() }, { p as String; 123 })
    p.<!UNRESOLVED_REFERENCE!>length<!> // Bad: p is Nothing? | (String & Nothing?) = Nothing?
    p?.<!UNRESOLVED_REFERENCE!>length<!> // Technically OK because p is null, but what is "length"?
}

fun test3(x: Any?) {
    var p: Any? = x
    p.<!UNRESOLVED_REFERENCE!>length<!> // Bad
    run2({ p = null; n() }, { p = ""; 123 })
    p.<!UNRESOLVED_REFERENCE!>length<!> // Bad: p can be null
    p?.<!UNRESOLVED_REFERENCE!>length<!> // Bad: KT-37838 -> OK: p is String | Nothing? = String?
}

interface I1 { val x: Int }
interface I2 { val y: Int }

fun test4(x: Any?) {
    x.<!UNRESOLVED_REFERENCE!>x<!> // Bad
    x.<!UNRESOLVED_REFERENCE!>y<!> // Bad
    run2(
        { x as I1; x.<!UNRESOLVED_REFERENCE!>y<!>; n() }, // Bad: may or may not be called first
        { x as I2; x.<!UNRESOLVED_REFERENCE!>x<!>; 123 } // Bad: may or may not be called first
    )
    x.<!UNRESOLVED_REFERENCE!>x<!> // Bad: KT-37838 -> OK: x is I1 & I2
    x.<!UNRESOLVED_REFERENCE!>y<!> // Bad: KT-37838 -> OK: x is I1 & I2
}

fun test5(x: Any?, q: String?) {
    var p: Any? = x
    p.<!UNRESOLVED_REFERENCE!>length<!> // Bad
    run2({ p as Int; 123 }, { p = q; n() })
    p.<!UNRESOLVED_REFERENCE!>length<!> // Bad: p is String? | (String? & Int) = String?
    p?.<!UNRESOLVED_REFERENCE!>length<!> // Bad: KT-37838 -> OK: p is String?
}

fun test6() {
    val x: String
    // not necessarily initialized in second lambda (may call in any order)
    run2({ x = ""; x.length }, { <!UNINITIALIZED_VARIABLE!>x<!>.length })
    x.length // initialized here
}

fun test7() {
    val x: Any? = ""
    val y: Any?
    run2({ y = x }, { })
    if (y is String) {
        x.length // ok - aliased
    }
}
