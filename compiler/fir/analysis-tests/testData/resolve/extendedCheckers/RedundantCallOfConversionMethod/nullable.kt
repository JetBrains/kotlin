// WITH_RUNTIME
// IS_APPLICABLE: false
fun foo(s: String?) {
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>t<!>: String = s.toString()<!>
}
