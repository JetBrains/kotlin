fun any(a : Any) {}

fun notAnExpression() {
    any(<!NO_SUPERTYPE, NO_SUPERTYPE, SUPER_NOT_AVAILABLE!>super<!>) // not an expression
    if (<!NO_SUPERTYPE, NO_SUPERTYPE, SUPER_NOT_AVAILABLE!>super<!>) {} else {} // not an expression
    val x = <!NO_SUPERTYPE, NO_SUPERTYPE, NO_SUPERTYPE, SUPER_NOT_AVAILABLE!>super<!> // not an expression
    when (1) {
        <!NO_SUPERTYPE, NO_SUPERTYPE, SUPER_NOT_AVAILABLE!>super<!> -> 1 // not an expression
        else -> {}
    }

}