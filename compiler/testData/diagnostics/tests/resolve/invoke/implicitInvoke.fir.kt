class B

class A {
    operator fun B.invoke() = 4
}

class X {
    operator fun invoke() = 3
}

fun test(a: A, b: B) {
    with (a) {
        b()
        (b)()
    }

    X()()
    val x = X()
    x()
    (x)()
}

fun test(c: () -> String, e: Int.() -> String) {
    c()
    (c)()

    3.<!UNRESOLVED_REFERENCE!>e<!>()
    3.(<!UNRESOLVED_REFERENCE!>e<!>)()
    with(3) {
        <!INAPPLICABLE_CANDIDATE!>e<!>()
        (<!INAPPLICABLE_CANDIDATE!>e<!>)()
    }
}
