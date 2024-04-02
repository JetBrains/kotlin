fun foo(): Int {
    val x: Any? = null
    val y = 2
    if (x == y) {
        return <!RETURN_TYPE_MISMATCH!>x <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>+<!> y<!>
    }
    return y
}
