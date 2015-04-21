open class C {
}

fun C.foo() {}

open class X {
    companion object : C() {}
}

open class Y {
    companion object : C() {}
}

fun bar() {
    val x = X
    x.foo()
    X.foo()
    (X <!USELESS_CAST!>as C<!>).foo()
    ((if (1<2) X else Y) <!USELESS_CAST!>as C<!>).foo()
}
