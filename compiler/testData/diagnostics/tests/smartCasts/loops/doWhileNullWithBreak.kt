fun bar(): Boolean { return true }

fun foo(s: String?): Int {
    do {
        if (bar()) break
    } while (s==null)
    // This call is unsafe due to break
    return s<!UNSAFE_CALL!>.<!>length
}