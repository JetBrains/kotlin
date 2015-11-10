fun foo(): Int {
    val x: Any? = null
    val y = 2
    if (x == y) {
        return <!DEBUG_INFO_SMARTCAST!>x<!> + y
    }
    return y
}