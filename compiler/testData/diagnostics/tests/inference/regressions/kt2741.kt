//KT-2741 Compiler can't infer a type of a function literal parameter when its body contains errors

package a

fun <T, R: Comparable<R>> Iterable<T>._sortBy(f: (T) -> R): List<T> = throw Exception()
fun <T> _arrayList(vararg values: T) : List<T> = throw Exception()

class _Pair<A>(val a: A)

fun test() {
    _arrayList(_Pair(1))._sortBy <!TYPE_MISMATCH, TYPE_MISMATCH!>{ it -> <!UNRESOLVED_REFERENCE!>xxx<!> }<!>
}
