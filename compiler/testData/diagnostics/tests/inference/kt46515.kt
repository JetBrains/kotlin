// WITH_STDLIB

class In<in K>()
fun <T, R : Comparable<R>, K> Iterable<T>.maxOf(selector: (T) -> R, vararg x: In<K>) {}

fun bar() {
    listOf(1, 2, 3).<!NONE_APPLICABLE!>maxOf<!> { <!UNRESOLVED_REFERENCE!>foo<!> }
    listOf(1, 2, 3).<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>maxOf<!>(<!TYPE_MISMATCH, TYPE_MISMATCH!>{ <!UNRESOLVED_REFERENCE!>foo<!> }<!>, In<String>(), In<Int>())
}