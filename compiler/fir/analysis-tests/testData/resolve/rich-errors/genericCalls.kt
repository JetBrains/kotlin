// RUN_PIPELINE_TILL: FRONTEND

error object E1
error object E2
error object E3

fun <T> foo1(t: T) {}
fun <T : Any? | E1 | E2 | E3> foo2(t: T) {}
fun <T : Any? | E1 | E2> foo3(t: T) {}
fun <T : E1 | E2> foo4(t: T) {}
fun <T : E1 | E2> foo5(t1: T, t2: T) {}
fun <T : E1 | E2, V : T> foo6(t1: T, t2: V) {}

fun bar() {
    <!CANNOT_INFER_PARAMETER_TYPE!>foo1<!>(<!ARGUMENT_TYPE_MISMATCH!>E1<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>foo1<!>(<!ARGUMENT_TYPE_MISMATCH!>E2<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>foo1<!>(<!ARGUMENT_TYPE_MISMATCH!>E3<!>)

    foo2(E1)
    foo2(E2)
    foo2(E3)

    foo3(E1)
    foo3(E2)
    <!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>foo3<!>(E3)

    foo4(E1)
    foo4(E2)
    <!INAPPLICABLE_CANDIDATE!>foo4<!>(E3)

    foo5(E1, E1)
    foo5(E1, E2)
    <!INAPPLICABLE_CANDIDATE!>foo5<!>(E1, E3)
    foo5(E2, E1)
    foo5(E2, E2)
    <!INAPPLICABLE_CANDIDATE!>foo5<!>(E2, E3)
    <!INAPPLICABLE_CANDIDATE!>foo5<!>(E3, E1)
    <!INAPPLICABLE_CANDIDATE!>foo5<!>(E3, E2)
    <!INAPPLICABLE_CANDIDATE!>foo5<!>(E3, E3)

    foo6(E1, E1)
    foo6(E1, E2)
    <!INAPPLICABLE_CANDIDATE!>foo6<!>(E1, E3)
    foo6(E2, E1)
    foo6(E2, E2)
    <!INAPPLICABLE_CANDIDATE!>foo6<!>(E2, E3)
    <!INAPPLICABLE_CANDIDATE!>foo6<!>(E3, E1)
    <!INAPPLICABLE_CANDIDATE!>foo6<!>(E3, E2)
    <!INAPPLICABLE_CANDIDATE!>foo6<!>(E3, E3)
}
