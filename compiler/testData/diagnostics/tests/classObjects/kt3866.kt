open class C {
}

fun C.foo() {}

open class X {
    default object : C() {}
}

open class Y {
    default object : C() {}
}

fun bar() {
    val x = X
    x.foo()
    X.foo()
    (X : C).foo()
    (X <!USELESS_CAST_STATIC_ASSERT_IS_FINE!>as C<!>).foo()
    ((if (1<2) X else Y) : C).foo()
}
