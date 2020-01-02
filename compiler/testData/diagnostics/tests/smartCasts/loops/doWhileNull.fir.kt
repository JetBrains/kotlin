fun foo(s: String?): Int {
    do {
    } while (s==null)
    return s.<!INAPPLICABLE_CANDIDATE!>length<!>
}