// CHECK_TYPE

fun tryFinally(x: Int?) {
    try {
    } finally {
        x!!
    }
    checkSubtype<Int>(x)
}

fun tryCatchFinally(x: Int?) {
    try {
        x!!
    } catch (e: Exception) {
        x!!
    } finally {
        checkSubtype<Int>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
        x!!
    }
    checkSubtype<Int>(x)
}
