fun foo(s: String?): Int {
    while (s==null) {
    }
    return s.<!INAPPLICABLE_CANDIDATE!>length<!>
}