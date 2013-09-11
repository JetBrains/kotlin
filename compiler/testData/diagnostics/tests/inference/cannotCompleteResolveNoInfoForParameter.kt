package f

fun f<T>(<!UNUSED_PARAMETER!>i<!>: Int, <!UNUSED_PARAMETER!>c<!>: Collection<T>): List<T> {throw Exception()}
fun f<T>(<!UNUSED_PARAMETER!>a<!>: Any, <!UNUSED_PARAMETER!>l<!>: List<T>): Collection<T> {throw Exception()}

fun test<T>(<!UNUSED_PARAMETER!>l<!>: List<T>) {
    <!CANNOT_COMPLETE_RESOLVE!>f<!>(1, emptyList())
}

fun emptyList<T>(): List<T> {throw Exception()}
