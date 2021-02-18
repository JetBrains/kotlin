fun Any.foo1() : (i : Int) -> Unit {
    return {}
}

fun test(a : Any) {
    a.<!INAPPLICABLE_CANDIDATE!>foo1<!>()()
}