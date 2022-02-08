// WITH_STDLIB
// IS_APPLICABLE: false
fun foo(s: String?) {
    val <!UNUSED_VARIABLE!>t<!>: String = s.toString()
}
