// ISSUE: KT-68613

interface Generic<out T>

typealias TA<K> = (String) -> Generic<K>

fun test(it: Any) {
    val that = it <!UNCHECKED_CAST!>as TA<<!REDUNDANT_PROJECTION!>out<!> Any><!>
}

fun rest(it: Any) {
    val that = it <!UNCHECKED_CAST!>as TA<<!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>in<!> Any><!>
}
