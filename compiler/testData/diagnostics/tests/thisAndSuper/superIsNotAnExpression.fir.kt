fun any(a : Any) {}

fun notAnExpression() {
    any(<!NO_SUPERTYPE, NO_SUPERTYPE!>super<!>) // not an expression
    if (<!NO_SUPERTYPE, NO_SUPERTYPE!>super<!>) {} else {} // not an expression
    val x = <!NO_SUPERTYPE, NO_SUPERTYPE, NO_SUPERTYPE!>super<!> // not an expression
    when (1) {
        <!NO_SUPERTYPE, NO_SUPERTYPE!>super<!> -> 1 // not an expression
        else -> {}
    }

}