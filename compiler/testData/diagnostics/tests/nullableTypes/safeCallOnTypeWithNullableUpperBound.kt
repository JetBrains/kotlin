fun <T> test(t: T): String? {
    if (t != null) {
        return t<!UNNECESSARY_SAFE_CALL!>?.<!>toString()
    }
    return <!DEBUG_INFO_CONSTANT!>t<!>?.toString()
}

fun <T> T.testThis(): String? {
    if (this != null) {
        return this<!UNNECESSARY_SAFE_CALL!>?.<!>toString()
    }
    return <!DEBUG_INFO_CONSTANT!>this<!>?.toString()
}