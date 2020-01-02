// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_VARIABLE

fun <T> foo(l: MutableList<T>): MutableList<T> = l
fun test(l: MutableList<out Int>) {
    val a: MutableList<out Int> = foo(l)
    val b = foo(l)
    b checkType { <!UNRESOLVED_REFERENCE!>_<!>< MutableList<out Int> >() }
}