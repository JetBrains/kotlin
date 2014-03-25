fun Boolean.plus(): Boolean {
    return false
}

fun foo(a: Boolean, b: Boolean) : Boolean {
    return !<caret>(+a || b)
}