package f

fun f<T>(<!UNUSED_PARAMETER!>i<!>: Int, <!UNUSED_PARAMETER!>t<!>: T, <!UNUSED_PARAMETER!>c<!>: MutableCollection<T>) {}
fun f<T>(<!UNUSED_PARAMETER!>a<!>: Any, <!UNUSED_PARAMETER!>t<!>: T, <!UNUSED_PARAMETER!>l<!>: MutableList<T>) {}

fun test(l: List<Int>) {
    <!NONE_APPLICABLE!>f<!>(1, "", l)
}
