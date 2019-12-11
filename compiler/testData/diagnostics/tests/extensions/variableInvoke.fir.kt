class A(foo: Int.() -> Unit) {
    init {
        4.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}

fun test(foo: Int.(String) -> Unit) {
    4.<!UNRESOLVED_REFERENCE!>foo<!>("")
    4.<!UNRESOLVED_REFERENCE!>foo<!>(p1 = "")
    4.<!UNRESOLVED_REFERENCE!>foo<!>(p2 = "")
}