// !WITH_NEW_INFERENCE
class A() {
    override fun equals(other : Any?) : Boolean = false
}

fun f(): Unit {
    var x: Int? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>1<!>
    x = null
    <!DEBUG_INFO_CONSTANT!>x<!> + 1
    <!DEBUG_INFO_CONSTANT!>x<!> <!INFIX_MODIFIER_REQUIRED!>plus<!> 1
    <!DEBUG_INFO_CONSTANT!>x<!> <!UNSAFE_OPERATOR_CALL!><<!> 1
    <!TYPE_MISMATCH!><!DEBUG_INFO_CONSTANT!>x<!> += 1<!>

    <!DEBUG_INFO_CONSTANT!>x<!> == 1
    <!DEBUG_INFO_CONSTANT!>x<!> != 1

    <!EQUALITY_NOT_APPLICABLE!>A() == 1<!>

    <!EQUALITY_NOT_APPLICABLE!><!DEBUG_INFO_CONSTANT!>x<!> === "1"<!>
    <!EQUALITY_NOT_APPLICABLE!><!DEBUG_INFO_CONSTANT!>x<!> !== "1"<!>

    <!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!><!DEBUG_INFO_CONSTANT!>x<!> === 1<!>
    <!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!><!DEBUG_INFO_CONSTANT!>x<!> !== 1<!>

    <!DEBUG_INFO_CONSTANT!>x<!><!UNSAFE_OPERATOR_CALL!>..<!>2
    <!TYPE_MISMATCH, DEBUG_INFO_CONSTANT!>x<!> in 1..2

    val y : Boolean? = true
    <!UNUSED_EXPRESSION!>false || <!TYPE_MISMATCH!>y<!><!>
    <!UNUSED_EXPRESSION!><!TYPE_MISMATCH!>y<!> && true<!>
    <!UNUSED_EXPRESSION!><!TYPE_MISMATCH!>y<!> && <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!><!>
}