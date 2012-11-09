package f

fun g<T>(<!UNUSED_PARAMETER!>i<!>: Int, <!UNUSED_PARAMETER!>a<!>: Any): List<T> {throw Exception()}
fun g<T>(<!UNUSED_PARAMETER!>a<!>: Any, <!UNUSED_PARAMETER!>i<!>: Int): Collection<T> {throw Exception()}

fun test<T>() {
    val <!UNUSED_VARIABLE!>c<!>: List<T> = <!CANNOT_COMPLETE_RESOLVE!>g<!>(1, 1)
}
