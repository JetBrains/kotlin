fun test(d: dynamic) {
    val a = arrayOf(1, 2, 3)

    d.foo(<!SPREAD_OPERATOR_IN_DYNAMIC_CALL!>*<!>d)
    d.foo(<!SPREAD_OPERATOR_IN_DYNAMIC_CALL!>*<!>a)
    d.foo(1, "2", <!SPREAD_OPERATOR_IN_DYNAMIC_CALL!>*<!>a)
    d.foo(1, *a) <!VARARG_OUTSIDE_PARENTHESES!>{ }<!>
    d.foo(*a) <!VARARG_OUTSIDE_PARENTHESES!>{ "" }<!>
    d.foo(<!SPREAD_OPERATOR_IN_DYNAMIC_CALL!>*<!>a, <!SPREAD_OPERATOR_IN_DYNAMIC_CALL!>*<!>a)
    d.foo(*a, *a) <!VARARG_OUTSIDE_PARENTHESES!>{ "" }<!>
    d.foo(<!SPREAD_OPERATOR_IN_DYNAMIC_CALL!>*<!>a, 1, { "" }, <!SPREAD_OPERATOR_IN_DYNAMIC_CALL!>*<!>a)
    d.foo(<!SPREAD_OPERATOR_IN_DYNAMIC_CALL!>*<!>a, 1)
    d.foo(<!SPREAD_OPERATOR_IN_DYNAMIC_CALL!>*<!>a, <!SPREAD_OPERATOR_IN_DYNAMIC_CALL!>*<!>a, { "" })

    bar(d)
    bar(d, d)
    bar(<!WRONG_OPERATION_WITH_DYNAMIC!>*d<!>)
    bar(<!WRONG_OPERATION_WITH_DYNAMIC!>*d<!>, <!WRONG_OPERATION_WITH_DYNAMIC!>*d<!>)
    bar(<!WRONG_OPERATION_WITH_DYNAMIC!>*d<!>, 23, <!WRONG_OPERATION_WITH_DYNAMIC!>*d<!>)
}

fun bar(vararg x: Int): Unit = TODO("$x")
