// ISSUE: KT-68613

interface Generic<out T>

typealias TA<K> = (String) -> Generic<K>
typealias RA<L> = TA<L>

fun rest(it: Any) = it <!UNCHECKED_CAST!>as RA<<!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>in<!> Any><!>
