class A() {
    override fun equals(other : Any?) : Boolean = false
}

fun f(): Unit {
    var x: Int? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>1<!>
    x = null
    x + 1
    <!DEBUG_INFO_CONSTANT!>x<!> <!INFIX_MODIFIER_REQUIRED!>plus<!> 1
    <!DEBUG_INFO_CONSTANT!>x<!> <!UNSAFE_OPERATOR_CALL!><<!> 1
    <!TYPE_MISMATCH!>x += 1<!>

    <!DEBUG_INFO_CONSTANT!>x<!> == 1
    <!DEBUG_INFO_CONSTANT!>x<!> != 1

    <!EQUALITY_NOT_APPLICABLE!>A() == 1<!>

    <!EQUALITY_NOT_APPLICABLE!><!DEBUG_INFO_CONSTANT!>x<!> === "1"<!>
    <!EQUALITY_NOT_APPLICABLE!><!DEBUG_INFO_CONSTANT!>x<!> !== "1"<!>

    <!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!><!DEBUG_INFO_CONSTANT!>x<!> === 1<!>
    <!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!><!DEBUG_INFO_CONSTANT!>x<!> !== 1<!>

    x<!UNSAFE_OPERATOR_CALL!>..<!>2
    <!DEBUG_INFO_CONSTANT, TYPE_MISMATCH!>x<!> in 1..2

    val y : Boolean? = true
    false || <!TYPE_MISMATCH!>y<!>
    <!TYPE_MISMATCH!>y<!> && true
    <!TYPE_MISMATCH!>y<!> && <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>
}
