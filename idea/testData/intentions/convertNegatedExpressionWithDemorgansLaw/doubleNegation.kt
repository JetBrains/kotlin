fun String.not(): Boolean {
    return isEmpty()
}

fun foo(a: Boolean, b: Boolean) : Boolean {
    return !<caret>(!"" || b)
}