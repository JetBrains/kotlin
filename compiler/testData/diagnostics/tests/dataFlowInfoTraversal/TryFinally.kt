fun tryFinally(x: Int?) {
    try {
    } finally {
        x!!
    }
    x : Int
}

fun tryCatchFinally(x: Int?) {
    try {
        x!!
    } catch (e: Exception) {
        x!!
    } finally {
        <!TYPE_MISMATCH!>x<!> : Int
        x!!
    }
    x : Int
}
