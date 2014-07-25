fun any(<!UNUSED_PARAMETER!>a<!> : Any) {}

fun notAnExpression() {
    any(<!SUPER_IS_NOT_AN_EXPRESSION!>super<!>) // not an expression
    if (<!SUPER_IS_NOT_AN_EXPRESSION!>super<!>) {} else {} // not an expression
    val <!UNUSED_VARIABLE!>x<!> = <!SUPER_IS_NOT_AN_EXPRESSION!>super<!> // not an expression
    when (1) {
        <!SUPER_IS_NOT_AN_EXPRESSION!>super<!> -> <!UNUSED_EXPRESSION!>1<!> // not an expression
        else -> {}
    }

}