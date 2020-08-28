// !WITH_NEW_INFERENCE
class A() {
    operator infix fun plus(i : Int) {}
    operator fun unaryMinus() {}
    operator infix fun contains(a : Any?) : Boolean = true
}

operator infix fun A.div(i : Int) {}
operator infix fun A?.times(i : Int) {}

fun test(x : Int?, a : A?) {
    x.<!NONE_APPLICABLE!>plus<!>(1)
    x?.plus(1)
    x <!NONE_APPLICABLE!>+<!> 1
    <!INAPPLICABLE_CANDIDATE!>-<!>x
    x.<!INAPPLICABLE_CANDIDATE!>unaryMinus<!>()
    x?.unaryMinus()

    a.<!INAPPLICABLE_CANDIDATE!>plus<!>(1)
    a?.plus(1)
    a <!INAPPLICABLE_CANDIDATE!>plus<!> 1
    a <!INAPPLICABLE_CANDIDATE!>+<!> 1
    <!INAPPLICABLE_CANDIDATE!>-<!>a
    a.<!INAPPLICABLE_CANDIDATE!>unaryMinus<!>()
    a?.unaryMinus()

    a.<!INAPPLICABLE_CANDIDATE!>div<!>(1)
    a <!INAPPLICABLE_CANDIDATE!>/<!> 1
    a <!INAPPLICABLE_CANDIDATE!>div<!> 1
    a?.div(1)

    a.times(1)
    a * 1
    a times 1
    a?.times(1)

    1 <!INAPPLICABLE_CANDIDATE!>in<!> a
    a <!INAPPLICABLE_CANDIDATE!>contains<!> 1
    a.<!INAPPLICABLE_CANDIDATE!>contains<!>(1)
    a?.contains(1)
}
