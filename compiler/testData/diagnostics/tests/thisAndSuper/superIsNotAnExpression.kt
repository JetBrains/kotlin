// FIR_IDENTICAL
fun any(a : Any) {}

fun notAnExpression() {
    any(<!SUPER_IS_NOT_AN_EXPRESSION!>super<!>) // not an expression
    if (<!SUPER_IS_NOT_AN_EXPRESSION!>super<!>) {} else {} // not an expression
    val x = <!SUPER_IS_NOT_AN_EXPRESSION!>super<!> // not an expression
    when (1) {
        <!SUPER_IS_NOT_AN_EXPRESSION!>super<!> -> 1 // not an expression
        else -> {}
    }

}
