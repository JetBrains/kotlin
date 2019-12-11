fun Any.foo1() : (i : Int) -> Unit {
    return {}
}

fun test(a : Any) {
    <!INAPPLICABLE_CANDIDATE!>a.foo1()()<!>
}