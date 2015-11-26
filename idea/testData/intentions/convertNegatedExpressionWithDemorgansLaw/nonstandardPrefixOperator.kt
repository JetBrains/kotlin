operator fun Boolean.unaryPlus(): Boolean {
    return false
}

fun foo(a: Boolean, b: Boolean) : Boolean {
    return !<caret>(+a || b)
}