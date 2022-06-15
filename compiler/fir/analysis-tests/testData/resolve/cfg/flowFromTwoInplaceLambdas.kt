// !DUMP_CFG
import kotlin.contracts.*

fun <T> n(): T? = null

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
    p<!UNSAFE_CALL!>.<!>length // Bad: p can be null
    p?.length // OK: p is String | Nothing? = String?
}

fun test3(x: Any?) {
    var p: Any? = x
    p.<!UNRESOLVED_REFERENCE!>length<!> // Bad
    run2({ p = null; n() }, { p = ""; 123 })
    p<!UNSAFE_CALL!>.<!>length // Bad: p can be null
    p?.length // OK: p is String | Nothing? = String?
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
    x.x // OK: x is I1 & I2
    x.y // OK: x is I1 & I2
}
