// RUN_PIPELINE_TILL: FRONTEND

error object E1
error object E2
error object E3

fun foo1(arg: Any?) {}
fun foo2(arg: Any) {}
fun foo3(arg: Int) {}
fun foo4(arg: Any? | E1) {}
fun foo5(arg: Int | E1) {}
fun foo6(arg: Any? | E1 | E2) {}
fun foo7(arg: Int | E1 | E2) {}
fun foo8(): E1 = E1
fun foo9(): Int | E2 = 4

fun bar(e1: E1, e2: E2, e3: E3) {
    <!INAPPLICABLE_CANDIDATE!>foo1<!>(e1)
    <!INAPPLICABLE_CANDIDATE!>foo1<!>(E1)

    <!INAPPLICABLE_CANDIDATE!>foo2<!>(e2)
    <!INAPPLICABLE_CANDIDATE!>foo2<!>(E2)

    <!INAPPLICABLE_CANDIDATE!>foo3<!>(e3)
    <!INAPPLICABLE_CANDIDATE!>foo3<!>(E3)

    foo4(e1)
    foo4(E1)
    <!INAPPLICABLE_CANDIDATE!>foo4<!>(e2)
    <!INAPPLICABLE_CANDIDATE!>foo4<!>(E2)

    foo5(e1)
    foo5(E1)
    <!INAPPLICABLE_CANDIDATE!>foo5<!>(e2)
    <!INAPPLICABLE_CANDIDATE!>foo5<!>(E2)

    foo6(e1)
    foo6(E1)
    foo6(e2)
    foo6(E2)
    <!INAPPLICABLE_CANDIDATE!>foo6<!>(e3)
    <!INAPPLICABLE_CANDIDATE!>foo6<!>(E3)

    foo7(e1)
    foo7(E1)
    foo7(e2)
    foo7(E2)
    <!INAPPLICABLE_CANDIDATE!>foo7<!>(e3)
    <!INAPPLICABLE_CANDIDATE!>foo7<!>(E3)

    <!INAPPLICABLE_CANDIDATE!>foo1<!>(foo8())
    <!INAPPLICABLE_CANDIDATE!>foo2<!>(foo8())
    <!INAPPLICABLE_CANDIDATE!>foo3<!>(foo8())
    foo4(foo8())
    foo5(foo8())
    foo6(foo8())
    foo7(foo8())

    <!INAPPLICABLE_CANDIDATE!>foo1<!>(foo9())
    <!INAPPLICABLE_CANDIDATE!>foo2<!>(foo9())
    <!INAPPLICABLE_CANDIDATE!>foo3<!>(foo9())
    <!INAPPLICABLE_CANDIDATE!>foo4<!>(foo9())
    <!INAPPLICABLE_CANDIDATE!>foo5<!>(foo9())
    foo6(foo9())
    foo7(foo9())
}
