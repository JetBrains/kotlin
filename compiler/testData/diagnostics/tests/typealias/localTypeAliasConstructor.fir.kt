// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class Cell<TC>(val x: TC)

fun <T> id(x: T): T {
    typealias C = Cell<T>
    class Local(val cell: C)
    val cx = <!UNRESOLVED_REFERENCE!>C<!>(x)
    val c: C = Local(cx).cell
    return c.<!UNRESOLVED_REFERENCE!>x<!>
}
