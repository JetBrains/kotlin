// !WITH_NEW_INFERENCE
class A() {
    operator infix fun plus(<!UNUSED_PARAMETER!>i<!> : Int) {}
    operator fun unaryMinus() {}
    operator infix fun contains(<!UNUSED_PARAMETER!>a<!> : Any?) : Boolean = true
}

operator infix fun A.div(<!UNUSED_PARAMETER!>i<!> : Int) {}
operator infix fun A?.times(<!UNUSED_PARAMETER!>i<!> : Int) {}

fun test(x : Int?, a : A?) {
    x<!OI;UNSAFE_CALL!>.<!><!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>plus<!>(1)
    x?.plus(1)
    x <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;UNSAFE_OPERATOR_CALL!>+<!> 1
    <!UNSAFE_CALL!>-<!>x
    x<!UNSAFE_CALL!>.<!>unaryMinus()
    x?.unaryMinus()

    a<!OI;UNSAFE_CALL!>.<!><!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>plus<!>(1)
    a?.plus(1)
    a <!UNSAFE_INFIX_CALL!>plus<!> 1
    a <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;UNSAFE_OPERATOR_CALL!>+<!> 1
    <!UNSAFE_CALL!>-<!>a
    a<!UNSAFE_CALL!>.<!>unaryMinus()
    a?.unaryMinus()

    a<!UNSAFE_CALL!>.<!>div(1)
    a <!UNSAFE_OPERATOR_CALL!>/<!> 1
    a <!UNSAFE_INFIX_CALL!>div<!> 1
    a?.div(1)

    a.times(1)
    a * 1
    a times 1
    a?.times(1)

    1 <!UNSAFE_OPERATOR_CALL!>in<!> a
    a <!UNSAFE_INFIX_CALL!>contains<!> 1
    a<!UNSAFE_CALL!>.<!>contains(1)
    a?.contains(1)
}
