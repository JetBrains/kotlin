fun tryFinally(x: Int?) {
    try {
    } finally {
        x!!
    }
    <!DEBUG_INFO_AUTOCAST!>x<!> : Int
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
    <!DEBUG_INFO_AUTOCAST!>x<!> : Int
}
