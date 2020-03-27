fun any(a : Any) {}

fun notAnExpression() {
    any(<!SUPER_NOT_AVAILABLE!>super<!>) // not an expression
    if (<!SUPER_NOT_AVAILABLE!>super<!>) {} else {} // not an expression
    val x = <!SUPER_NOT_AVAILABLE!>super<!> // not an expression
    when (1) {
        <!SUPER_NOT_AVAILABLE!>super<!> -> 1 // not an expression
        else -> {}
    }

}