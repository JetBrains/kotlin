// !CHECK_TYPE
// SKIP_TXT

fun foo(s: String) {
    s.isBlank()
    s.lines().checkType { _<List<String>>() }
    s.repeat(1)

    // We don't have `strip` extension, so leave it for a while in gray list
    s.<!DEPRECATION!>strip<!>()
}
