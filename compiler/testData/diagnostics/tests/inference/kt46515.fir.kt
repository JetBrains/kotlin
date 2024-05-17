// WITH_STDLIB

class In<in K>()
fun <T, R : Comparable<R>, K> Iterable<T>.maxOf(selector: (T) -> R, vararg x: In<K>) {}

fun bar() {
    listOf(1, 2, 3).<!OVERLOAD_RESOLUTION_AMBIGUITY!>maxOf<!> <!UNRESOLVED_REFERENCE!>{ <!UNRESOLVED_REFERENCE!>foo<!> }<!>
    listOf(1, 2, 3).<!CANNOT_INFER_PARAMETER_TYPE, INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>maxOf<!>(<!UNRESOLVED_REFERENCE!>{ <!UNRESOLVED_REFERENCE!>foo<!> }<!>, In<String>(), In<Int>())
}