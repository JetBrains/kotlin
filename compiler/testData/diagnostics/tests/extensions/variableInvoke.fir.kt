class A(foo: Int.() -> Unit) {
    init {
        4.foo()
    }
}

fun test(foo: Int.(String) -> Unit) {
    4.foo("")
    4.<!INAPPLICABLE_CANDIDATE!>foo<!>(p1 = "")
    4.foo(p2 = "")
}