fun bar(): Boolean { return true }

fun foo(s: String?): Int {
    while (s==null) {
        if (bar()) break
    }
    // Call is unsafe due to break
    return s.<!INAPPLICABLE_CANDIDATE!>length<!>
}