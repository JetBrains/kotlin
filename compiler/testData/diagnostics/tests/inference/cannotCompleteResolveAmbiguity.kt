// !WITH_NEW_INFERENCE
package f

fun <T> g(<!UNUSED_PARAMETER!>i<!>: Int, <!UNUSED_PARAMETER!>a<!>: Any): List<T> {throw Exception()}
fun <T> g(<!UNUSED_PARAMETER!>a<!>: Any, <!UNUSED_PARAMETER!>i<!>: Int): Collection<T> {throw Exception()}

fun <T> test() {
    val <!UNUSED_VARIABLE!>c<!>: List<T> = <!CANNOT_COMPLETE_RESOLVE!>g<!>(1, 1)
}
