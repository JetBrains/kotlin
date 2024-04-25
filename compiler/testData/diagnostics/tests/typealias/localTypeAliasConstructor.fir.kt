// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class Cell<TC>(val x: TC)

fun <T> id(x: T): T {
    typealias C = Cell<T>
    class Local(val cell: <!UNRESOLVED_REFERENCE!>C<!>)
    val cx = <!UNRESOLVED_REFERENCE!>C<!>(x)
    val c: <!UNRESOLVED_REFERENCE!>C<!> = Local(cx).cell
    return c.<!UNRESOLVED_REFERENCE!>x<!>
}
