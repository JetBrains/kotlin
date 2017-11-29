// !WITH_NEW_INFERENCE
fun <T> test(t: T): T {
    if (t != null) {
        return t<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    }
    return <!ALWAYS_NULL!>t<!>!!
}

fun <T> T.testThis(): String {
    if (this != null) {
        return this<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.toString()
    }
    <!NI;UNREACHABLE_CODE!>return<!> this!!<!NI;UNREACHABLE_CODE!>.toString()<!>
}

