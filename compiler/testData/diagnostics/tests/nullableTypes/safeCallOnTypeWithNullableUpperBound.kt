fun <T> test(t: T): String? {
    if (t != null) {
        return <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>t<!UNNECESSARY_SAFE_CALL!>?.<!>toString()<!>
    }
    return <!DEBUG_INFO_CONSTANT!>t<!>?.toString()
}

fun <T> T.testThis(): String? {
    if (this != null) {
        return <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>this<!UNNECESSARY_SAFE_CALL!>?.<!>toString()<!>
    }
    return this?.toString()
}
