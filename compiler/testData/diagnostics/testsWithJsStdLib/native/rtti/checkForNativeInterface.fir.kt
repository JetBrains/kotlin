external interface I

fun box(a: Any, b: Any): Boolean {
    return a is I && b !is I
}
