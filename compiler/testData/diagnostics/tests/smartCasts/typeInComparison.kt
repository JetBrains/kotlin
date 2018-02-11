// !WITH_NEW_INFERENCE
fun foo(): Int {
    val x: Any? = null
    val y = 2
    if (x == y) {
        return x <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>+<!> y
    }
    return y
}