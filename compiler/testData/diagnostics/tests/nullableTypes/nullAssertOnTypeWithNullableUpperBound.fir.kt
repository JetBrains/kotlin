fun <T> test(t: T): T {
    if (t != null) {
        return t<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    }
    return t!!
}

fun <T> T.testThis(): String {
    if (this != null) {
        return this<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.toString()
    }
    return this!!.toString()
}

