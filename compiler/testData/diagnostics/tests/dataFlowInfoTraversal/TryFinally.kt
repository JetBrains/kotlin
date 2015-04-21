// !CHECK_TYPE

fun tryFinally(x: Int?) {
    try {
    } finally {
        x!!
    }
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
}

fun tryCatchFinally(x: Int?) {
    try {
        x!!
    } catch (e: Exception) {
        x!!
    } finally {
        checkSubtype<Int>(<!TYPE_MISMATCH!>x<!>)
        x!!
    }
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>x<!>)
}
