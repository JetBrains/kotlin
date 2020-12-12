// !CHECK_TYPE

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
        <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(x)
        x!!
    }
    checkSubtype<Int>(x)
}
