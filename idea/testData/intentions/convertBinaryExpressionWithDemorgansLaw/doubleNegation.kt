operator fun String.not(): Boolean {
    return length == 0
}

fun foo(a: Boolean, b: Boolean) : Boolean {
    return !(<caret>!"" || b)
}
