//KT-2741 Compiler can't infer a type of a function literal parameter when its body contains errors
package a

fun <T, R: Comparable<R>> Iterable<T>._sortBy(<!UNUSED_PARAMETER!>f<!>: (T) -> R): List<T> = throw Exception()
fun <T> _arrayList(vararg <!UNUSED_PARAMETER!>values<!>: T) : List<T> = throw Exception()

class _Pair<A>(val a: A)

fun test() {
    _arrayList(_Pair(1))._sortBy { <!UNUSED_PARAMETER!>it<!> -> <!UNRESOLVED_REFERENCE!>xxx<!> }
}
