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

fun test2(x: Any?) {
    var p: Any? = x
    p.<!UNRESOLVED_REFERENCE!>length<!> // Bad
    run2({ p = null; n() }, { p as String; 123 })
    p<!UNSAFE_CALL!>.<!>length // Bad: p can be null
    p?.length // OK: p is either null or String
}

fun test3(x: Any?) {
    var p: Any? = x
    p.<!UNRESOLVED_REFERENCE!>length<!> // Bad
    run2({ p = null; n() }, { p = ""; 123 })
    p<!UNSAFE_CALL!>.<!>length // Bad: p can be null
    p?.length // OK: p is either null or String
}
