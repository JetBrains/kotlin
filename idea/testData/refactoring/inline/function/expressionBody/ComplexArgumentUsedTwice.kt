fun <caret>f(p: Int) = p + p

fun complexFun(): Int {
}

fun g(): Int {
    return f(complexFun())
}