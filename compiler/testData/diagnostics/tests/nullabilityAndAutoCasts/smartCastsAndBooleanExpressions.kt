fun foo(b: Boolean?, c: Boolean) {
    if (b != null && <!DEBUG_INFO_AUTOCAST!>b<!>) {}
    if (b == null || <!DEBUG_INFO_AUTOCAST!>b<!>) {}
    if (b != null) {
        if (<!DEBUG_INFO_AUTOCAST!>b<!> && c) {}
        if (<!DEBUG_INFO_AUTOCAST!>b<!> || c) {}
    }
}
