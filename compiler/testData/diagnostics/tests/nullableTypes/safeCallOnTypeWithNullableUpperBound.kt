fun <T> test(t: T): String? {
    if (t != null) {
        return t<!UNNECESSARY_SAFE_CALL!>?.<!>toString()
    }
    return t?.toString()
}

fun <T> T.testThis(): String? {
    if (this != null) {
        return this<!UNNECESSARY_SAFE_CALL!>?.<!>toString()
    }
    return this?.toString()
}